package external;
// 外部enternal package访问外部资源，并不是jupyter本身的资源
// search通过http的请求与ticketmaster连接，连接的结果转换成json格式，返回给调用者
// item这个helper就是为了把调用的结果转化为需要的形式
// 之后讲如何利用Item创建自己的API放到servlet上作为自己的服务
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	//要访问的 events。Jason
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	// 搜索的filter
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	// 自己的APIkey
	private static final String API_KEY = "YuxLckREtCVJZpSPPJBgaMmB5EGLjr01";
	
	/**
	 * Helper methods
	 */

	//  {
	//    "name": "laioffer",
              //    "id": "12345",
              //    "url": "www.laioffer.com",
	//    ...
	//    "_embedded": {
	//	    "venues": [
	//	        {
	//		        "address": {
	//		           "line1": "101 First St,",
	//		           "line2": "Suite 101",
	//		           "line3": "...",
	//		        },
	//		        "city": {
	//		        	"name": "San Francisco"
	//		        }
	//		        ...
	//	        },
	//	        ...
	//	    ]
	//    }
	//    ...
	//  }
	
	//前三个是helper function来找到三个层级比较深的数据
	//ticket master address venue给的是一个json array多个地址，可以改成List<String>，此方法只返回了第一个地点
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			
			if (!embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				//或直接写成 if(venus.lenghth() > 0 ; venus.getJSONObject(0)
				for (int i = 0; i < venues.length(); ++i) {
					JSONObject venue = venues.getJSONObject(i);
					
					StringBuilder sb = new StringBuilder();
					
					if (!venue.isNull("address")) {
						JSONObject address = venue.getJSONObject("address");
						
						if (!address.isNull("line1")) {
							sb.append(address.getString("line1"));
						}
						if (!address.isNull("line2")) {
							sb.append(" ");
							sb.append(address.getString("line2"));
						}
						if (!address.isNull("line3")) {
							sb.append(" ");
							sb.append(address.getString("line3"));
						}
					}
					
					if (!venue.isNull("city")) {
						JSONObject city = venue.getJSONObject("city");
						
						if (!city.isNull("name")) {
							sb.append(" ");
							sb.append(city.getString("name"));
						}
					}
					
					if (!sb.toString().equals("")) {
						return sb.toString();
					}
				}
			}
		}

		return "";
	}


	// {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("images")) {
			JSONArray images = event.getJSONArray("images");
			
			for (int i = 0; i < images.length(); ++i) {
				JSONObject image = images.getJSONObject(i);
				
				if (!image.isNull("url")) {
					return image.getString("url");
				}
			}
		}
		return "";
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	// 一个活动可以有多个标签
	// exception是一个object，JSONException是一个exception子类，局部异常，用getitemlist来接exception
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (!event.isNull("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			for (int i = 0; i < classifications.length(); i++) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (!segment.isNull("name")) {
						String name = segment.getString("name");
						categories.add(name);
					}
				}
			}
		}
		return categories;
	}

	// Convert JSONArray to a list of item objects.
	// 通过search方法连接ticketmasterAPI获取events一个json array后，用getitemlist方法获得item我们想关注的8个内容
	// search调用getitemlist，
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();

		for (int i = 0; i < events.length(); ++i) {
			// 获得每个json array数组里面的每个event
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			
			// json object类提供的方法isnull来判断某个key是否存在
			// 如果name键存在则setname
			// 这5个数据在events的第一层，参见ticket master event search的response格式
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			
			//rating已经不存在了，之后要在前端更改
			if (!event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			
			builder.setCategories(getCategories(event));
			builder.setAddress(getAddress(event));
			builder.setImageUrl(getImageUrl(event));
			
			itemList.add(builder.build());
		}

		return itemList;
	}
	
	// 通过ticket master API经纬度搜索，可选提供keyword作为filter
	// 用java自己的数据结构来返回
	public List<Item> search(double lat, double lon, String keyword) {
	 	// Encode keyword in url since it may contain special characters
		if (keyword == null) {
			keyword= DEFAULT_KEYWORD;
		}
		try {
			// 对字符 encode，万一是中文字符
			// 用的是 http 传输模式，所以用 URLEncoder
			// 发送给Ticketmaster的时候用发的是URL
			keyword= java.net.URLEncoder.encode(keyword, "UTF-8");
		} catch (Exception e) {
			// 本来需要处理这个 exception 的
			e.printStackTrace();
		}
		
		// Convert lat/lon to geo hash
		String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
		
		// 创建request的URL
		// Make your url query part like: "apikey=12345&geoPoint=abcd&keyword=music&radius=50"
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);
		try {
			// Open a HTTP connection between your Java application and TicketMaster based on url
			// 拼接出URL，变成整体的URL，connection就是连接程序和ticket master
			// 用open connection，返回一个url connection的类型，待会用到的response code只有httpurlconnection支持
			// 根据 url不同，生成不同类型 的connection， 从最基本的URL类型转换成specific的httpURLconnection
			HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
			// Set request method to GET
			//获得connection后发送请求并获取结果
			// 这一行没写
			connection.setRequestMethod("GET");
			//要不要加connection.connect(); 使用 connect 方法建立到远程对象的实际连接。
			// Send request to TicketMaster and get response, response code could be
			// returned directly
			// response body is saved in InputStream of connection.
			// 远程对象变为可用。远程对象的头字段和内容变为可访问
			// 打开connection以后请求并没有发出去，用getresponsecode后会发送请求并返回请求结果
			int responseCode = connection.getResponseCode();
			// 输出结果用于debug
			System.out.println("\nSending 'GET' request to URL : " + URL + "?" + query);
			System.out.println("Response Code : " + responseCode);
//			if (responseCode != 200) {
//				//
//			}
			// Now read response body to get events data
			// BufferedReader in就是一个指针，readline才是将这些数据抄下来
			// 获取响应正文
			// InputStreamReader是一个工具，可以读这些所有数据
			// BufferedReader可以一行一行地读
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// 获取的结果现在是一个写成json类型的string
			JSONObject obj = new JSONObject(response.toString());
			// 检测 embedded是否存在，不存在表示没有查到结果，返回的是空结果
			// embedded是ticket master中定义的应该返回的东西
			if (obj.isNull("_embedded")) {
				return new ArrayList<>();
			}
			//想找的是embedded里面的event
			JSONObject embedded = obj.getJSONObject("_embedded");
			// events是jsonarray类型，是search的结果
			JSONArray events = embedded.getJSONArray("events");
			return getItemList(events);
			//没有处理exception，前面getitemlist抛出的各种异常就在这里接住
		} catch (Exception e) {
			e.printStackTrace();
		}
		// return jsonarray而不是null，这样caller就不用先判断是不是null
		return new ArrayList<>();
	 }

	//一个helper function，获得json object后把内容输出来，用于debug，检测search 获取的额结果是不是正确
	private void queryAPI(double lat, double lon) {
		List<Item> itemList = search(lat, lon, null);
		try {
			for (Item item : itemList) {
				JSONObject jsonObject = item.toJSONObject();
				System.out.println(jsonObject);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Main entry for sample TicketMaster API requests.
	 */
	// 打出来的是一个raw string ,只需要取我们需要的项
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}
}




