package net.wendal.nutzbook.module.yvr;

import java.io.File;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.wendal.nutzbook.bean.User;
import net.wendal.nutzbook.bean.UserProfile;
import net.wendal.nutzbook.module.BaseModule;
import net.wendal.nutzbook.service.UserService;
import net.wendal.nutzbook.util.Toolkit;

import org.apache.shiro.SecurityUtils;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.lang.meta.Email;
import org.nutz.lang.random.R;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.GET;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.POST;
import org.nutz.mvc.annotation.Param;
import org.nutz.trans.Atom;
import org.nutz.trans.Trans;

@At("/yvr/u")
@IocBean
public class YvrUserModule extends BaseModule {

	private static final Log log = Logs.get();
	
	protected byte[] emailKEY = R.sg(24).next().getBytes();
	
	@Inject
	protected UserService userService;
	
	@At("/?")
	@Ok("raw")
	public String userHome() {
		return "<h1>暂未实现</h1>";
	}
	

	@Ok("raw:jpg")
	@At("/?/avatar")
	public File userAvatar(){
		return new File(Mvcs.getServletContext().getRealPath("/rs/user_avatar/none.jpg"));
	}
	
	/**
	 * 邮件回调的入口
	 * @param token 包含用户名和邮箱地址的加密内容
	 */
	@GET
	@At("/signup/?")
	@Ok("raw")
	public Object signup(String token) {
		try {
			token = Toolkit._3DES_decode(emailKEY, Toolkit.hexstr2bytearray(token));
		} catch (Exception e) {
			return "非法token,请重新注册";
		}
		final String[] tmps = token.split(",");
		long time = Long.parseLong(tmps[0]);
		if ((System.currentTimeMillis()/1000) - time > 24*60*60) {
			return "该链接已经过期";
		}
		// 再次检查用户名
		if (0 != dao.count(User.class, Cnd.where("name", "=", tmps[1]))) {
			return "用户名已被占用";
		}
		if (0 != dao.count(UserProfile.class, Cnd.where("email", "=", tmps[2]))) {
			return "Email地址已被占用";
		}
		Trans.exec(new Atom(){
			public void run() {
				User user = userService.add(tmps[1], tmps[3]);
				UserProfile profile = new UserProfile();
				profile.setEmail(tmps[2]);
				profile.setEmailChecked(true);
				profile.setNickname(tmps[1]);
				profile.setUser(user);
				profile.setUserId(user.getId());
				dao.fastInsert(profile);
			}
		});
		return "注册成功,可以登陆了";
	}

	protected static Pattern P_USERNAME = Pattern.compile("[a-z][a-z0-9]{4,10}");
	protected static Pattern P_PASSWORD = Pattern.compile("(?=^.{8,16}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$");
	
	@POST
	@At
	@Ok("json")
	public Object signup(@Param("email")String email, 
						@Param("username")String username,
						@Param("password")String password,
			HttpServletRequest req) {
		if (Strings.isBlank(password) || !P_PASSWORD.matcher(password).find()) {
			return ajaxFail("密码强度不够!!");
		}
		if (Strings.isBlank(username) || !P_USERNAME.matcher(username.toLowerCase()).find()) {
			return ajaxFail("用户名不合法");
		}
		int count = dao.count(User.class, Cnd.where("name", "=", username));
		if (count != 0) {
			return ajaxFail("用户名已经存在");
		}
		if (email.contains(",")||password.contains(",")) {
			return ajaxFail("不允许使用英文逗号");
		}
		try {
			new Email(email);
		} catch (Exception e) {
			return ajaxFail("Email地址不合法");
		}
		count = dao.count(UserProfile.class, Cnd.where("email", "=", email));
		if (count != 0) {
			return ajaxFail("Email已经存在");
		}
		try {
			String token = String.format("%s,%s,%s,%s", System.currentTimeMillis()/1000, username, email, password);
			token = Toolkit._3DES_encode(emailKEY, token.getBytes());
			String url = req.getRequestURL() + "/" + token;
			String html = "<div>如果无法点击,请拷贝一下链接到浏览器中打开<p/>注册链接 %s</div>";
			html = String.format(html, url, url);
			if (emailService.send(email, "Nutz社区注册邮件", html))
				return ajaxOk("请查收邮件,点击邮件中的链接即可完成注册");
		} catch (Exception e) {
			log.debug("有点问题", e);
		}
		return ajaxOk("发送邮件失败");
	}
	
	@At("/oauth/github")
	@Ok("->:/oauth/github")
	public void oauth(String type, HttpServletRequest req, HttpSession session){
		String url = req.getHeader("Rerefer");
		if (url == null)
			url = "/yvr/list";
		session.setAttribute("oauth.return.url", url);
	}

	@At
	@Ok(">>:/yvr")
	public void logout() {
		SecurityUtils.getSubject().logout();
		HttpSession session = Mvcs.getHttpSession(false);
		if (session != null)
			session.invalidate();
	}
}
