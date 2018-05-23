package cn.itcast.bos.web.action.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;


import org.apache.struts2.ServletActionContext;
import org.springframework.data.domain.Page;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

public class BaseAction<T> extends ActionSupport implements ModelDriven<T> {
	protected T model;//泛型不能实例化,使用真实类型进行实例化(泛型转换)
	
	@Override
	public T getModel() {
		return  model;
	}
	//当baseAction被子类调用的时候,实例化构造方法
	public BaseAction(){
		Type type = this.getClass().getGenericSuperclass();//从子类获取父类baseAction<Standard>
		ParameterizedType parameterizedType = (ParameterizedType) type;
		Class entityClass = (Class) parameterizedType.getActualTypeArguments()[0];
		try {
			model = (T) entityClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	//属性驱动
	protected int page;//当前页
	protected int rows;//当前页最多显示的记录数

	public void setPage(int page) {
		this.page = page;
	}
	public void setRows(int rows) {
		this.rows = rows;
	}
	
	//将分页查询的结果,存放到栈顶
	protected void pushPageDataToValueStack(Page<T> pageData){
		Map<String, Object> map = new HashMap<>();
		map.put("total", pageData.getTotalElements());
		map.put("rows", pageData.getContent());
		ServletActionContext.getContext().getValueStack().push(map);
	}
	//将对象或者集合压入栈顶,存放到栈顶
	protected void pushObjectToValueStack(Object obj){
		ServletActionContext.getContext().getValueStack().push(obj);
	}
	

}
