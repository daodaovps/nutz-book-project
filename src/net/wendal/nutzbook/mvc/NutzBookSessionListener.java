package net.wendal.nutzbook.mvc;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import net.wendal.nutzbook.bean.SysLog;
import net.wendal.nutzbook.service.syslog.SysLogService;
import net.wendal.nutzbook.util.Toolkit;

import org.nutz.mvc.Mvcs;

public class NutzBookSessionListener implements HttpSessionListener {

	SysLogService sysLogService;

	public void sessionCreated(HttpSessionEvent event) {
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		int uid;
		try {
			uid = Toolkit.uid();
		} catch (Exception e) {
			Integer i = (Integer) event.getSession().getAttribute("me");
			if (i == null)
				return;
			uid = i;
		}
		if (uid < 1)
			return;
		if (sysLogService == null) {
			sysLogService = Mvcs.ctx().getDefaultIoc().get(SysLogService.class);
		}
		SysLog syslog = SysLog.c("method", "用户退出", null, uid, "用户登出");
		sysLogService.async(syslog);
	}

}
