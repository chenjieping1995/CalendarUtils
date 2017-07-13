package com.congshao;

import java.util.Calendar;


 /**
  * 公历日期转换为农历的工具类
  * @author c81023005 chenjieping
  */
public class SolarToLunarUtils {

	//最小日期为公历1970-1-1 即农历1969-11-24
	private static final int MIN_YEAR = 1969;
	private static final int MIN_DATE_MONTH = 11;
	private static final int MAX_YEAR = 2100;

    private static final long MIN_TIME_MILLIS, MAX_TIME_MILLIS;
    
    /**
     * 农历年数据表(1900-2100年)
     * 每个农历年用16进制来表示，解析时转为2进制
     * 二进码长度为16位,如解码为15位高位补0,(当解码为17位时,最高位1即所闰月位大月,反之为小月)
     * 前12位分别表示12个农历月份的大小月，1是大月，0是小月
     * 最后4位表示闰月，转为十进制后即为闰月值，例如0110，则为闰6月
     */
    private final static int[] LUNAR_INFO = {
    		0x20, /* 1969 */
            0x96d0, 0x4dd5, 0x4ad0, 0xa4d0, 0xd4d4, 0xd250, 0xd558, 0xb540, 0xb6a0, 0x195a6, /* 1970-1979 */
            0x95b0, 0x49b0, 0xa974, 0xa4b0, 0xb27a, 0x6a50, 0x6d40, 0xaf46, 0xab60, 0x9570, /* 1980-1989 */
            0x4af5, 0x4970, 0x64b0, 0x74a3, 0xea50, 0x6b58, 0x5ac0, 0xab60, 0x96d5, 0x92e0, /* 1990-1999 */
            0xc960, 0xd954, 0xd4a0, 0xda50, 0x7552, 0x56a0, 0xabb7, 0x25d0, 0x92d0, 0xcab5, /* 2000-2009 */
            0xa950, 0xb4a0, 0xbaa4, 0xad50, 0x55d9, 0x4ba0, 0xa5b0, 0x15176, 0x52b0, 0xa930, /* 2010-2019 */
            0x7954, 0x6aa0, 0xad50, 0x5b52, 0x4b60, 0xa6e6, 0xa4e0, 0xd260, 0xea65, 0xd530, /* 2020-2029 */
            0x5aa0, 0x76a3, 0x96d0, 0x26fb, 0x4ad0, 0xa4d0, 0x1d0b6, 0xd250, 0xd520, 0xdd45, /* 2030-2039 */
            0xb5a0, 0x56d0, 0x55b2, 0x49b0, 0xa577, 0xa4b0, 0xaa50, 0x1b255, 0x6d20, 0xada0, /* 2040-2049 */
            0x14b63, 0x9370, 0x49f8, 0x4970, 0x64b0, 0x168a6, 0xea50, 0x6aa0, 0x1a6c4, 0xaae0, /* 2050-2059 */
            0x92e0, 0xd2e3, 0xc960, 0xd557, 0xd4a0, 0xda50, 0x5d55, 0x56a0, 0xa6d0, 0x55d4, /* 2060-2069 */
            0x52d0, 0xa9b8, 0xa950, 0xb4a0, 0xb6a6, 0xad50, 0x55a0, 0xaba4, 0xa5b0, 0x52b0, /* 2070-2079 */
            0xb273, 0x6930, 0x7337, 0x6aa0, 0xad50, 0x14b55, 0x4b60, 0xa570, 0x54e4, 0xd160, /* 2080-2089 */
            0xe968, 0xd520, 0xdaa0, 0x16aa6, 0x56d0, 0x4ae0, 0xa9d4, 0xa2d0, 0xd150, 0xf252, /* 2090-2099 */
            0xd520 /* 2100 */
    };

    private final static String[] LUNAR_MONTH_ARRAYS = {"正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
    private final static String[] LUNAR_DAY_ARRAYS = {"初", "十", "廿", "卅"};
    private final static String[] NUMBERS = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    static {
    	Calendar calendar = Calendar.getInstance();
    	//公历1970-1-1 即农历1969-11-24
        calendar.set(MIN_YEAR+1, 0, 1, 0, 0, 0);
        MIN_TIME_MILLIS = calendar.getTimeInMillis();
        //公历2100-12-31 即农历2100-12-1
        calendar.set(MAX_YEAR, 11, 31, 23, 59, 59);
        MAX_TIME_MILLIS = calendar.getTimeInMillis();
    }

    /**
     * 根据时间毫秒值转换 农历
     *
     * @param timeInMillis 时间毫秒值
     * @return Lunar 农历对象
     */
    public static Lunar converterDate(long timeInMillis) {
        if (timeInMillis < MIN_TIME_MILLIS || timeInMillis > MAX_TIME_MILLIS) {
            throw new RuntimeException("日期超出农历计算范围,-->minDate:1970-1-1 maxDate 2100-12-31");
        }
        Lunar lunar = new Lunar();
        // 距离起始日期间隔的总天数 间隔天数和目标日期差一天
        long offset = (timeInMillis - MIN_TIME_MILLIS) / (24 * 60 * 60 * 1000);
        // 默认农历起始年为1969年，且由此开始推算农历年份
        int lunarYear = MIN_YEAR;
        // 推算出输入日期所在农历年
        while (true) {
            int daysInLunarYear = getLunarYearDays(lunarYear);
            if (offset > daysInLunarYear) {
                offset -= daysInLunarYear;
                lunarYear++;
            } else {
                break;
            }
        }
        lunar.year = lunarYear;
        // 获取该农历年的闰月月份
        int leapMonth = getLunarLeapMonth(lunarYear);
        // 没有闰月则不是闰年
        lunar.leapMonth = leapMonth;
        // 默认农历月为正月，且由此开始推算农历月
        int lunarMonth = lunarYear == MIN_YEAR ? 11 : 1;
        int daysInLunarMonth;
        // 递减每个农历月的总天数,确定农历月份,先计算非闰月后计算闰月
        while (true) {
            if (lunarMonth == leapMonth) { 
            	// 该农历年闰月的天数,先算正常月再算闰月 如果闰一月 先减去一月再减去闰一月
                daysInLunarMonth = getLunarDays(lunarYear, lunarMonth);
                if (offset > daysInLunarMonth) {
                	//剩余天数>当月天数
                    offset -= daysInLunarMonth;	//减去差额
                    if (offset > getLunarLeapDays(lunarYear)) {
                    	//剩余天数>闰月天数
                        offset -= getLunarLeapDays(lunarYear);	//减去闰月天数
                        lunarMonth++;	//月份+1
                    } else {
                        lunarMonth = lunarYear;	//标记闰月为当前年份
                        break;
                    }
                } else {
                    break;
                }
            } else { 
            	// 该农历年正常农历月份的天数
                daysInLunarMonth = getLunarDays(lunarYear, lunarMonth);
                if (offset > daysInLunarMonth) {
                	//剩余天数>当月天数
                    offset -= daysInLunarMonth;	//减去差额
                    lunarMonth++;	//月份+1
                } else {
                    break;
                }
            }
        }
        lunar.month = lunarMonth;
        lunar.day = (lunarYear == MIN_YEAR && lunarMonth == MIN_DATE_MONTH) ? (int) Math.abs(-offset + -MIN_DATE_MONTH) : (int) offset;
        return lunar;
    }

    /**
     * 获取某农历年的总天数
     *
     * @param lunarYear 农历年份
     * @return 该农历年的总天数
     */
    private static int getLunarYearDays(int lunarYear) {
        if (lunarYear == MIN_YEAR) {
        	// 农历的1969年只有35天的数据
            return 35;
        }
        // 按小月计算,农历年最少有12 * 29 = 348天
        int daysInLunarYear = 348;
        // 遍历前12位
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            // 每个大月累加一天
            daysInLunarYear += ((LUNAR_INFO[lunarYear - MIN_YEAR] & i) != 0) ? 1 : 0;
        }
        // 加上闰月天数
        daysInLunarYear += getLunarLeapDays(lunarYear);

        return daysInLunarYear;
    }

    /**
     * 获取某农历年闰月的总天数
     *
     * @param lunarYear 农历年份
     * @return 该农历年闰月的天数, 无闰月返回0 (闰月的天数等于所润月的天数)
     */
    private static int getLunarLeapDays(int lunarYear) {
        // 计算所闰月为大月还是小月 如果该年的二进制码为17位,最高位为1则该年闰月为大月,反之则为小月（2017/2055）
        // 若该年没有闰月,返回0
        return getLunarLeapMonth(lunarYear) > 0 ? ((LUNAR_INFO[lunarYear - MIN_YEAR] & 0x10000) > 0 ? 30 : 29) : 0;
    }

    /**
     * 获取某农历年闰月月份
     *
     * @param lunarYear 农历年份
     * @return 该农历年闰月的月份, 四位二进制码即为闰月的月份, 0为不闰月
     */
    private static int getLunarLeapMonth(int lunarYear) {
        // 匹配后4位
        int leapMonth = LUNAR_INFO[lunarYear - MIN_YEAR] & 0xf;
        leapMonth = (leapMonth == 0xf ? 0 : leapMonth);
        if (leapMonth > 12) {
        	//闰月月份不能大于12 否则数据肯定是错误的
            throw new RuntimeException(lunarYear + "年数据错误,lunarYear:" + Integer.toBinaryString(LUNAR_INFO[lunarYear - MIN_YEAR]));
        }
        return leapMonth;
    }

    /**
     * 获取某农历年某月的总天数
     *
     * @param lunarYear 农历年份
     * @return 该农历年某月的天数
     */
    private static int getLunarDays(int lunarYear, int month) {
        if (lunarYear == MIN_YEAR && month == 11) {
        	// 农历的1969年11月有6天数据在范围内
            return 6;
        }
        return (LUNAR_INFO[lunarYear - MIN_YEAR] & (0x10000 >> month)) != 0 ? 30 : 29;
    }
    
    
    /**
     * 用于获取中国的传统节日
     *
     * @param month 农历的月
     * @param day   农历日
     * @return 中国传统节日
     */
    public static String getLunarHoliday(int year, int month, int day) {
        String message = "";
        if (month == 1 && day == 1) {
            message = "春节";
        } else if (month == 1 && day == 15) {
            message = "元宵节";
        } else if (month == 5 && day == 5) {
            message = "端午节";
        } else if (month == 7 && day == 7) {
            message = "七夕";
        } else if (month == 8 && day == 15) {
            message = "中秋节";
        } else if (month == 9 && day == 9) {
            message = "重阳节";
        } else if (month == 12 && day == 8) {
            message = "腊八节";
        } else {
            if (month == 12) {
                if ((((getLunarDays(year, month) == 29) && day == 29))
                        || ((((getLunarDays(year, month) == 30) && day == 30)))) {
                    message = "除夕";
                }
            }
        }
        return message;
    }


    public static class Lunar {
        public int year;
        public int month;
        public int day;
        public int leapMonth;

        // 若当前月为闰月，则month值year值
        public String getMonth() {
            return month > LUNAR_MONTH_ARRAYS.length ? "闰" + LUNAR_MONTH_ARRAYS[leapMonth - 1] : LUNAR_MONTH_ARRAYS[month - 1];
        }

        public String getDay() {
            int result = day / 10;
            return LUNAR_DAY_ARRAYS[(result == 1 && day % 10 == 0) ? result - 1 : result] +
                    (day % 10 == 0 ? NUMBERS[NUMBERS.length - 1] : NUMBERS[Math.abs(day - result * 10) - 1]);
        }

        @Override
        public String toString() {
            return "Lunar{" +
                    "年=" + year +
                    ", 月=" + getMonth() +
                    ", 日=" + day +
                    ", 闰月=" + leapMonth +
                    ", 月/日:" + getMonth() + "月" + getDay() +
                    '}';
        }
    }
}