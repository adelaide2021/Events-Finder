package rpc;

import jakarta.servlet.ServletException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;
// import external.TicketMasterAPI;

// 把ticketmaster api的结果通过web service返回回去
// 把json转为item然后返回的时候再转换回json，这样中间item便于放到数据库中保存，用java自己的数据类型存储到数据库，分析用户的行为
/**
 * Servlet implementation class SearchItem
 */
// 
@WebServlet("/search") //反射
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONArray array = new JSONArray();
		try {
			String userId = request.getParameter("user_id");
			double lat = Double.parseDouble(request.getParameter("lat"));
			double lon = Double.parseDouble(request.getParameter("lon"));
			String keyword = request.getParameter("term");
			//更新成用dbconnection实现这个功能，mysql的实现就放在dbconnection下面
			//这样就把找到的item存在数据库中了
			DBConnection connection = DBConnectionFactory.getConnection();
			List<Item> items = connection.searchItems(lat, lon, keyword);
	 		
//			// 获取lat longh后就可以调用已经实现的ticket master api的方法来做搜索
//			
//			TicketMasterAPI tmAPI = new TicketMasterAPI();
//			List<Item> items = tmAPI.search(lat, lon, keyword);
	 		Set<String> favorite = connection.getFavoriteItemIds(userId);
			//connection.close();
	 		connection.close();
			for (Item item : items) {
				// 获取所有返回结果写到jsonarray
				JSONObject obj = item.toJSONObject();
				//判断下是否加过item到favorite list中
				// Check if this is a favorite one.
				// This field is required by frontend to correctly display favorite items.
				obj.put("favorite", favorite.contains(item.getItemId()));
				array.put(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 把获取到的jsonarray写回到response中
		RpcHelper.writeJsonArray(response, array);	
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
