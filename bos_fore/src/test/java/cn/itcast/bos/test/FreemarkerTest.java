package cn.itcast.bos.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;



public class FreemarkerTest {
	
	@Test
	public void testOutput() throws Exception{
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_22);
		configuration.setDirectoryForTemplateLoading(new File("D:\\weblx\\bos_fore\\src\\main\\webapp\\WEB-INF\\template"));
		//获取模板对象
		Template template = configuration.getTemplate("hello.ftl");
		Map<String, Object> map = new HashMap<>();
		map.put("title", "黑马小黑");
		map.put("msg", "这是第一个Freemarker案例");
		
		template.process(map, new PrintWriter(System.out));
		
	}
}
