package portrait;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.google.common.io.Files;

import bg.portrait.serpwow.MetaImageSerpwow;

class SerpwowTest {
	 String text="";
	@Test
	void test() throws Exception{
		assertTrue(true);
		Path path = Paths.get(this.getClass().getResource("/serpwow.json").toURI());
		
		List<String> lines  =Files.readLines( path.toFile() , Charset.defaultCharset());
		lines.stream().forEach(e->text+=e+"\n");
		System.out.println(text);
		JSONObject jsonResponse = new JSONObject(text);
		JSONArray jsonArray = jsonResponse.getJSONArray("inline_images");
		System.out.println("inline_images : length :"+jsonArray.length());
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject json = jsonArray.getJSONObject(i);
			MetaImageSerpwow meta = new MetaImageSerpwow(json);
			meta.store();
		}
		
		
	}

}
