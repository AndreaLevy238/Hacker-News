package com.example.andrea.hackernews;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
{
   private ListView listOfArticles;
   private ArrayAdapter<String> titleArrayAdapter;
   private List<String> articleTitles;
   private List<String> articleURLS;


   private final static String DBName = "Articles";
   private final static String tableName = "articles";
   private final static String article_id = "article_id";
   private final static String article_title = "title";
   private final static String article_url = "url";
   private final static String article_content = "content";
   public SQLiteDatabase articlesDB;



   private static final int NUM_ARTICLES = 20;
//   public static HashMap<Integer, String> articleTitleHash = new HashMap<>(NUM_ARTICLES);
//   public static HashMap<Integer, String> articleURLHash = new HashMap<>(NUM_ARTICLES);

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      articleTitles = new ArrayList<>(NUM_ARTICLES);
      articleURLS = new ArrayList<>(NUM_ARTICLES);

      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      listOfArticles = (ListView) findViewById(R.id.list_of_articles);
      articlesDB = this.openOrCreateDatabase(DBName, MODE_PRIVATE, null);
      createListView();
      new DownloadTask().execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
   }

   public void createListsFromDB()
   {
      try
      {
         Cursor c = articlesDB.rawQuery("SELECT * FROM " + tableName + " ORDER BY " + article_id + " DESC", null);
         int titleIndex = c.getColumnIndex(article_title);
         int urlIndex = c.getColumnIndex(article_url);
         boolean hasNext = c.moveToFirst();

         //clear titles & urls in case there is something inside
         articleTitles.clear();
         articleURLS.clear();

         while (hasNext)
         {

            articleTitles.add(c.getString(titleIndex));
            articleURLS.add(c.getString(urlIndex));

            hasNext = c.moveToNext();

         }
         c.close();
         titleArrayAdapter.notifyDataSetChanged();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }



   public void createListView()
   {

      titleArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, articleTitles);
      listOfArticles.setAdapter(titleArrayAdapter);
      listOfArticles.setOnItemClickListener(new AdapterView.OnItemClickListener()
      {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
         {
            Intent readerIntent = new Intent(getApplicationContext(), ViewArticle.class);
            readerIntent.putExtra("url", articleURLS.get(position));
            startActivity(readerIntent);
         }
      });
      createListsFromDB();
   }


   private class DownloadTask extends AsyncTask<String, Void, String>
   {
      private URL url;
      private HttpURLConnection urlConnection;


      private String readURL() throws IOException
      {
         String result = "";
         urlConnection = (HttpURLConnection) url.openConnection();
         InputStreamReader reader = new InputStreamReader(urlConnection.getInputStream());
         int data = reader.read();
         while (data != -1)
         {
            char cur = (char) data;
            result += cur;
            data = reader.read();
         }
         return  result;
      }


      @Override
      protected String doInBackground(String... urls)
      {
         String result = "";
         try
         {
            url = new URL(urls[0]);
            result = readURL();
            createDB(result);

         }
         catch (JSONException | IOException e)
         {
            e.printStackTrace();
         }


         return result;
      }
      public void createDB(String allIDs) throws JSONException, IOException
      {
         articlesDB.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (id INTEGER PRIMARY KEY, "
                 + article_id + " INTEGER, " + article_title + " VARCHAR, " + article_url + " VARCHAR, "
                 + article_content + "VARCHAR)");

         JSONArray idArray = new JSONArray(allIDs);
         articlesDB.execSQL("DELETE FROM " + tableName);
         for (int i = 0; i < NUM_ARTICLES; i++)
         {

            url = new URL("https://hacker-news.firebaseio.com/v0/item/" + idArray.getString(i) + ".json?print=pretty");

            String articleInfo = readURL();
            JSONObject articleJSON = new JSONObject(articleInfo);
            String articleTitle = articleJSON.getString("title");
            String articleURL = articleJSON.getString("url");
            String sql = "INSERT INTO " + tableName + " (" + article_id + ", " + article_title + ", " + article_url + ") VALUES (?, ?, ?)";
            SQLiteStatement statement = articlesDB.compileStatement(sql);
            statement.bindString(1, idArray.getString(i));
            statement.bindString(2, articleTitle);
            statement.bindString(3, articleURL);


            statement.execute();
         }

      }

      @Override
      protected void onPostExecute(String s)
      {
         super.onPostExecute(s);
         createListsFromDB();
         Log.i("UI", "updated");
      }
   }


}
