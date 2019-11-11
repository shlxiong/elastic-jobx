package com.dangdang.ddframe.job.util;

import java.text.ParseException;
import java.util.Date;

import org.quartz.impl.triggers.CronTriggerImpl;

public final class QuartzUtils {
	
	public static String checkCronExpress(String cron) {
    	CronTriggerImpl trigger = new CronTriggerImpl();
        try {
        	cron = cron.replace("？", "?").replace("＊", "*").replace("／", "/");  //全角字符
            trigger.setCronExpression(cron);
            Date date = trigger.computeFirstFireTime(null);
            if (date == null || date.before(new Date())) {
            	throw new IllegalArgumentException("执行时间("+date+")早于当前日期");
            }
            return cron;
        } catch (Exception e) {
        	throw new IllegalArgumentException("Cron表达式不正确：" + e.getMessage());
        } finally {
        	trigger = null;
        }
    }
	
	public static long getIntervalMils(String cron) {
		CronTriggerImpl trigger = new CronTriggerImpl();
        try {
        	cron = cron.replace("？", "?").replace("＊", "*").replace("／", "/");  //全角字符
            trigger.setCronExpression(cron);
            Date date = trigger.computeFirstFireTime(null);
            Date date2 = trigger.getFireTimeAfter(date);
            return date2.getTime() - date.getTime();
        } catch (Exception e) {
        	throw new IllegalArgumentException("Cron表达式不正确：" + e.getMessage());
        } finally {
        	trigger = null;
        }
	}
	
	public static void main(String[] args) throws ParseException {
		String cron = "0 8/30 * * * ?";
		System.out.println(getIntervalMils(cron));
	}

}
