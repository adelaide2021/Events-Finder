package entity;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//获取完数据清理一下，清理成java class的field的结构，方便存入数据库
public class Item {
	private String itemId;
	private String name;
	private double rating;
	private String address;
	private Set<String> categories;
	private String imageUrl;
	private String url;
	private double distance;
	
	// 此类创建这些field是用于读取的，写用另外的方法
	public String getItemId() {
		return itemId;
	}
	public String getName() {
		return name;
	}
	public double getRating() {
		return rating;
	}
	public String getAddress() {
		return address;
	}
	public Set<String> getCategories() {
		return categories;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public String getUrl() {
		return url;
	}
	public double getDistance() {
		return distance;
	}

	//把数据转换成json object的方法
	//最终要作为servlet的service返回给前段程序，前端程序需要一个json object
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("item_id", itemId);
			obj.put("name", name);
			obj.put("rating", rating);
			obj.put("address", address);
			obj.put("categories", new JSONArray(categories));
			obj.put("image_url", imageUrl);
			obj.put("url", url);
			obj.put("distance", distance);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	//生成这些item，有很多个组合，不同组合都用constructor实现太麻烦，用builderpattern这个inner class实现，因为是private constructor
	//用utility的class创建要创建的类，这个class里面可以设置默认值
	//static是必须的，否则要创建itembuilder的instance，而创建itembuilder instance又需要先创建item，死循环
	public static class ItemBuilder {
		public void setItemId(String itemId) {
			this.itemId = itemId;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setRating(double rating) {
			this.rating = rating;
		}
		public void setAddress(String address) {
			this.address = address;
		}
		public void setCategories(Set<String> categories) {
			this.categories = categories;
		}
		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public void setDistance(double distance) {
			this.distance = distance;
		}
		private String itemId;
		private String name;
		private double rating;
		private String address;
		private Set<String> categories;
		private String imageUrl;
		private String url;
		private double distance;
		
		// build方法最终生成item class的instance
		public Item build() {
			return new Item(this);
		}	
	}
	
	/**
	 * This is a builder pattern in Java.
	 */
	// item constructor，就是获取builder值后，用builder给出的值初始化每个field
	// 避免需要写很多constructor的问题
	// private，只能通过itembuilder来创建这个类，从封装性来讲不能变成public，
	// 用builder创建item而不是用constructor创建
	private Item(ItemBuilder builder) {
		this.itemId = builder.itemId;
		this.name = builder.name;
		this.rating = builder.rating;
		this.address = builder.address;
		this.categories = builder.categories;
		this.imageUrl = builder.imageUrl;
		this.url = builder.url;
		this.distance = builder.distance;
	}
}
