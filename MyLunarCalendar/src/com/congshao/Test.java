package com.congshao;

import java.util.Calendar;

import com.congshao.SolarToLunarUtils;;

public class Test {

	public static void main(String[] args) {
		SolarToLunarUtils mTest = new SolarToLunarUtils();
		Calendar mCalendar = Calendar.getInstance();
		System.out.println(mTest.converterDate(mCalendar.getTimeInMillis()).toString());
	}

}
