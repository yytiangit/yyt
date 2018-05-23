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
import cn.itcast.bos.domain.take_delivery.Promotion;
import cn.itcast.bos.page.PageBean;
import cn.itcast.bos.web.action.common.BaseAction;
import freemarker.template.Configuration;
import freemarker.template.Template;

@ParentPackage("json-default")
@Namespace("/")
@Controller
@Scope("prototype")
public class PromotionAction extends BaseAction<Promotion> {
	 @Action(value = "promotion_pageQuery",results={@Result(name="success",type="json")})
	 public String pageQuery(){
	   
		 PageBean<Promotion> pageData = WebClient.create(Constants.BOS_MANAGEMENT_URL+"/services/promotionService/findPageQuery?page="+ page + "&rows=" + rows)
				    .type(MediaType.APPLICATION_JSON)
				    .accept(MediaType.APPLICATION_JSON)
				    .get(PageBean.class);
		 
		 Map<String, Object> result = new HashMap<>();
		 result.put("totalCount", pageData.getTotalElements());
		 result.put("pageData", pageData.getContents());
		 pushObjectToValueStack(result);
		 return SUCCESS;
	 }
	 @Action(value="promotion_showDetail")
	 public String showDetail() throws Exception{
		 Integer id = model.getId();
		 //先判断id是否对应html是否存在,如果存在,直接返回
		 String htmlRealPath = ServletActionContext.getServletContext().getRealPath("/freemarker");
		 File htmlFile = new File(htmlRealPath+"/"+id+".html");
		 
		 //判断html是否存在
		 if (!htmlFile.exists()) {
			 Configuration configuration = new Configuration(Configuration.VERSION_2_3_22);
				configuration.setDirectoryForTemplateLoading(new File(ServletActionContext.getServletContext().getRealPath("/freemarker")));
				//获取模板对象
				Template template = configuration.getTemplate("promotion_detail.ftl");
				Promotion promotion = WebClient.create(Constants.BOS_MANAGEMENT_URL+"/services/promotionService/findById?id="+ id)
					    .type(MediaType.APPLICATION_JSON)
					    .accept(MediaType.APPLICATION_JSON)
					    .get(Promotion.class);
	
				Map<String, Object> map = new HashMap<>();
				map.put("promotion", promotion);
				
				//写入文件的编码
				template.process(map, new OutputStreamWriter(new FileOutputStream(htmlFile),"utf-8"));
				
				//页面解码
				ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
				
				//发送出去写入文件
				FileUtils.copyFile(htmlFile, ServletActionContext.getResponse().getOutputStream());
				
		}
		 
		 
		 
		 return NONE;
	 }
}
