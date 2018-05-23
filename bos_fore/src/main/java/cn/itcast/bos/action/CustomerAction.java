package cn.itcast.bos.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Controller;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;

import cn.itcast.bos.constant.Constants;
import cn.itcast.bos.utils.MailUtils;
import cn.itcast.bos.utils.SmsDemoUtils;
import cn.itcast.bos.web.action.common.BaseAction;
import cn.itcast.crm.domain.Customer;
  
@ParentPackage("json-default")
@Namespace("/")
@Actions   //表示全局跳转
@Controller
@Scope(value="prototype") //多实例
public class CustomerAction extends BaseAction<Customer> {
	
	private String activecode;
	public void setActivecode(String activecode) {
		this.activecode = activecode;
	}
	
	@Autowired
	@Qualifier("jmsQueueTemplate")
	private JmsTemplate jmsTemplate;
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	//注册-发送短信验证
	@Action(value="customer_sendsms",results={@Result(name="success",type="json")})
	public String sendsms() throws Exception{
		String telephone = model.getTelephone();
		//获取验证码
		final String number = SmsDemoUtils.getNumber();
		//将验证码存放到session中,用于在注册的时候的校验
		ServletActionContext.getRequest().getSession().setAttribute("number", number);
		//设置有效时间
		ServletActionContext.getRequest().getSession().setMaxInactiveInterval(1*60);
		
		//发送短信
//		SendSmsResponse sendSms = SmsDemoUtils.sendSms(telephone, number);
//		String code = sendSms.getCode();
		
		jmsTemplate.send("bos_sms30",new MessageCreator() {
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				MapMessage mapMessage = session.createMapMessage();
				mapMessage.setString("telephone", model.getTelephone());
				mapMessage.setString("number", number);
				return mapMessage;
			}
		});
		
		
		//假如成功了
		/*String code = "OK";
		if (code.equals("OK")) {
			System.out.println("当前电话号: "+telephone+"发送成功");
			Map<String, Object> map = new HashMap<>();
			map.put("success", number);
			pushObjectToValueStack(map);
		}else {
			throw new RuntimeException("当前电话号:"+telephone+"发送短信失败");
		}*/
		return SUCCESS;
	}
	//属性驱动
	private String checknum;
	
	public void setChecknum(String checknum) {
		this.checknum = checknum;
	}
	
	//注册
	@Action(value="customer_regist",results={@Result(name="success", type="redirect",location="./signup-success.html"),
			@Result(name="input",type="redirect",location="./signup.html")})
	public String regist() throws MessagingException{
		//判断短信验证码是否过期
		String number = (String) ServletActionContext.getRequest().getSession().getAttribute("number");
		//页面输入的验证码有误
		if (number == null || !checknum.equals(number)) {
			System.out.println("验证码输入有误或者验证码超过60秒已经过去!");
			return INPUT;
		}
		//判断手机号是不存在
		Customer customer = WebClient.create(Constants.CRM_MANAGEMENT_URL+"/services/customerService/customer/telephone/"+model.getTelephone())
		.type(MediaType.APPLICATION_JSON)
		.accept(MediaType.APPLICATION_JSON)
		.get(Customer.class);
		if (customer != null) {
			System.out.println("当前手机号已经注册，无需再次注册");
			return INPUT;
		}
		
		
		
		
		//测试
		Customer cust = model;
		System.out.println(cust);
		WebClient.create(Constants.CRM_MANAGEMENT_URL+"/services/customerService/customer/save")
			.type(MediaType.APPLICATION_JSON)
			.post(model);
		
		//生成32位随机数字
		String actioncode = RandomStringUtils.randomNumeric(32);
		//将激活码保存到redis设置24小时失效
		redisTemplate.opsForValue().set(model.getTelephone(), actioncode,24,TimeUnit.HOURS);
		//调用mailutils发送激活邮件
		String content = "尊敬的客户您好，请于24小时内，进行邮箱账户的绑定，点击下面地址完成绑定:<br/><a href='"
				+ MailUtils.activeUrl + "?telephone=" + model.getTelephone()
				+ "&activecode=" + actioncode + "'>速运快递邮箱绑定地址</a>";
		MailUtils.sendMail("快递激活邮件", content, model.getEmail(), "");
		System.out.println("注册成功!");
		return SUCCESS;
	}
	
	@Action("customer_activeMail")
	public String activeMail() throws Exception{
		ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
		//判断激活码是否有效
		String activeCodeRedis = redisTemplate.opsForValue().get(model.getTelephone());
		if (activeCodeRedis == null || !activecode.equals(activeCodeRedis)) {
			ServletActionContext.getResponse().getWriter().println("激活码无效，请登录系统，重新绑定邮箱！");
			return NONE;
		}else{
			//激活码有效
			//判断是否重复绑定,即T_CUSTOMER的TYPE字段是否为1
			
			Customer customer = WebClient.create(Constants.CRM_MANAGEMENT_URL+"/services/customerService/customer/telephone/"+model.getTelephone())
					.type(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.get(Customer.class);
			
			//更新type字段为1
			if (customer!=null&&customer.getType()==1) {
				// 已经绑定过
				ServletActionContext.getResponse().getWriter().println("邮箱已经绑定过，无需重复绑定！");
				return NONE;
			}else{
				
				WebClient.create(Constants.CRM_MANAGEMENT_URL+"/services/customerService/customer/updatetype/"+model.getTelephone())
							.type(MediaType.APPLICATION_JSON)
							.put(null);
				ServletActionContext.getResponse().getWriter().println("邮箱绑定成功！");
			}
		}
		//清空手机
		redisTemplate.delete(model.getTelephone());
		return NONE;
	}
	@Action(value="customer_login",results={@Result(name="input",location="login.html",type="redirect"),
			@Result(name="success",location="index.html#/myhome",type="redirect")})
	public String login(){
		String telephone = model.getTelephone();
		String password = model.getPassword();
		
		
		Customer customer = WebClient.create(Constants.CRM_MANAGEMENT_URL+"/services/customerService/findByTelephoneAndPassword?telephone="+telephone+"&password="+password)
		.accept(MediaType.APPLICATION_JSON)
		.type(MediaType.APPLICATION_JSON)
		.get(Customer.class);
		
		
		if (customer==null) {
			System.out.println("手机号是："+telephone+"，密码是："+password+"的账号输入有误！");
			return INPUT;
		}else{
			if (model.getType()==1) {
				System.out.println("手机号是："+telephone+"，邮箱是："+customer.getEmail()+"的账号没有被激活！");
				return INPUT;
			}
			ServletActionContext.getRequest().getSession().setAttribute("customer", customer);
			
		}
		
		return SUCCESS;
	}
	
	
}
