package net.wendal.nutzbook.mvc;

import java.io.IOException;

import javax.servlet.http.HttpSession;

import org.nutz.json.Json;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.ActionFilter;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.view.VoidView;

public class CsrfActionFilter implements ActionFilter {

	public org.nutz.mvc.View match(org.nutz.mvc.ActionContext ac) {
		HttpSession session = Mvcs.getHttpSession(false);
		if (session != null) {
			String csrf = (String) session.getAttribute("_csrf");
			if (csrf != null) {
				String _csrf = (String) ac.getRequest().getParameter("_csrf");
				if (csrf.equals(_csrf)) {
					return null;
				}
			}
		}

		NutMap re = new NutMap().setv("ok", false).setv("msg", "csrf错误,请刷新页面后重试");
		try {
			Json.toJson(ac.getResponse().getWriter(), re);
		} catch (IOException e) {
		}
		return new VoidView();
	}
}
