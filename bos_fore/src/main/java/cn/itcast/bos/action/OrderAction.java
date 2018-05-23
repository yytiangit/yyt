package cn.itcast.bos.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.itcast.bos.constant.Constants;
import cn.itcast.bos.domain.base.Area;
import cn.itcast.bos.domain.take_delivery.Order;
import cn.itcast.bos.domain.take_delivery.Promotion;
import cn.itcast.bos.page.PageBean;
import cn.itcast.bos.web.action.common.BaseAction;
import cn.itcast.crm.domain.Customer;
import freemarker.template.Configuration;
import freemarker.template.Template;

@ParentPackage("json-default")
@Namespace("/")
@Controller
@Scope("prototype")
public class OrderAction extends BaseAction<Order> {
	
	private String sendAreaInfo;//发件人
	
	private String recAreaInfo;//收件人

	public void setSendAreaInfo(String sendAreaInfo) {
		this.sendAreaInfo = sendAreaInfo;
	}

	public void setRecAreaInfo(String recAreaInfo) {
		this.recAreaInfo = recAreaInfo;
	}
	
	@Action(value="order_save",results={@Result(name="success",type="redirect",location="./index.html")})
	public String save(){
//		System.out.println(model);
		//手动封装area关联
		Area sendArea = new Area();
		String[] sendAreaData = sendAreaInfo.split("/");
		sendArea.setProvince(sendAreaData[0]);
		sendArea.setCity(sendAreaData[1]);
		sendArea.setDistrict(sendAreaData[2]);
		
		Area recArea = new Area();
		String[] recAreaData = recAreaInfo.split("/");
		recArea.setProvince(recAreaData[0]);
		recArea.setCity(recAreaData[1]);
		recArea.setDistrict(recAreaData[2]);
		
		model.setSendArea(sendArea);//寄件人省市区信息
		model.setRecArea(recArea);//收件人省市区信息
		
		//关联当前登录客户
		Customer customer = (Customer) ServletActionContext.getRequest().getSession().getAttribute("customer");
		model.setCustomer_id(customer.getId());
		
		//调用webService将数据传递bos_management系统中
		
		WebClient.create(Constants.BOS_MANAGEMENT_URL+"/services/orderService/order/save")
		.type(MediaType.APPLICATION_JSON)
		.post(model);
		System.out.println("客户下单成功!...");	
		return SUCCESS;
	}
	
	
	
	
}
