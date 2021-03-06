package com.maubis.scarlet.base.nino;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class JsonHelper {

    private int imgWidth;
    private int imgHeight;
    public JsonHelper(int width, int height) {
        imgWidth = width;
        imgHeight = height;

        Log.i("ABBYY W", String.valueOf(imgWidth));
        Log.i("ABBYY H", String.valueOf(imgHeight));
    }

    public JSONObject createJsonTemplate(JSONArray lines, JSONArray images) throws JSONException {
        JSONObject jo = new JSONObject();
        try {
            jo.put("version", "3.0.0");

            JSONObject meta = new JSONObject();
            meta.put("platform", "android");
            meta.put("verison", "6.2.6");
            meta.put("createdAt", "2019-03-13T00:01:02+03:00");
            jo.put("meta", meta);

            JSONObject image = new JSONObject();
            image.put("type", "image/jpeg");
            image.put("width", imgWidth);
            image.put("height", imgHeight);
            jo.put("image", image);

            JSONArray operations = new JSONArray();
            //JSONObject transform = new JSONObject();
            //JSONObject orientation = new JSONObject();
            //JSONObject adjustments = new JSONObject();

            JSONObject operations_sprite = new JSONObject();
            operations_sprite.put("type", "sprite");

            JSONObject operations_sprite_options = new JSONObject();

            JSONArray operations_sprite_option_sprites = new JSONArray();

            handleTexts(operations_sprite_option_sprites, lines);
            handleImages(operations_sprite_option_sprites, images);

            operations_sprite_options.put("sprites", operations_sprite_option_sprites);
            operations_sprite.put("options", operations_sprite_options);
            operations.put(operations_sprite);

            jo.put("operations", operations);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo;
    }

    private void handleTexts(JSONArray operations_sprite_option_sprites, JSONArray lines) throws JSONException {
        for(int i = 0; i < lines.length(); i++) {
            JSONObject entry = lines.getJSONObject(i);
            String text = entry.getString("text");

            double left = entry.getDouble("left") / imgWidth;
            double bottom = entry.getDouble("bottom") / imgHeight;
            double right = entry.getDouble("right") / imgWidth;
            double top = entry.getDouble("top") / imgHeight;

            double width = right - left;
            double height = bottom - top;

            Log.d("ATTEMPTING: TEXT", "text: " + text + " l: " + left + " r: " + right +
                    " b: " + bottom + " t: " + top + " mW: " + width + " fs: " + height);
            double xToPut;
            double yToPut;
            double margin = (double) (Math.abs(imgWidth - imgHeight) / 2);
            if(imgWidth > imgHeight){
                xToPut = left + width/2;
                yToPut = (margin / imgWidth) + (top + height/2);
            }else{
                xToPut = (margin / imgHeight) + (left + width/2);
                yToPut = top + height/2;
            }

            int lineCount = text.split("\\r?\\n").length - 1;
            double fontSize = (height/lineCount)*(imgHeight/1200.0) / 1.61803398875;
            Log.d("ATTEMPTING: TEXT", " x: " + xToPut + " y: " + yToPut + " w: " + width
                    + " fs: " + fontSize);
            JSONObject jsonTextObject = getTextJson(text, xToPut, yToPut, width, fontSize);
            operations_sprite_option_sprites.put(jsonTextObject);
        }
    }

    private void handleImages(JSONArray operations_sprite_option_sprites, JSONArray images) throws JSONException {
        for(int i = 0; i < images.length(); i++) {
            JSONObject entry = images.getJSONObject(i);
            double left = entry.getDouble("left") / imgWidth;
            double bottom = entry.getDouble("bottom") / imgHeight;
            double right = entry.getDouble("right") / imgWidth;
            double top = entry.getDouble("top") / imgHeight;

            double width = right - left;
            double height = bottom - top;

            Log.d("ATTEMPTING: IMAGE", " l: " + left + " r: " + right +
                    " b: " + bottom + " t: " + top + " mW: " + width + " fs: " + height);
            double xToPut;
            double yToPut;
            double margin = (double) (Math.abs(imgWidth - imgHeight) / 2);
            if(imgWidth > imgHeight){
                xToPut = left + width/2;
                yToPut = (margin / imgWidth) + top + height/2;
            }else{
                xToPut = (margin / imgHeight) + left + width/2;
                yToPut = bottom + height/2;
            }
            String identifier = "image" + i;
            JSONObject jsonImageObject = getImageJson(identifier, xToPut, yToPut, width, height);
            operations_sprite_option_sprites.put(jsonImageObject);
        }
    }

    private JSONObject getTextJson(String text, double x, double y, double maxWidth, double height) throws JSONException {
        JSONObject jto = new JSONObject();
        jto.put("type", "text");

        JSONObject options = new JSONObject();
        options.put("text", text);
        options.put("fontSize", height);//0.01);
        options.put("fontIdentifier", "imgly_font_fira_sans_regular");
        options.put("alignment", "left");

        JSONObject color = new JSONObject();
        JSONArray rgba = new JSONArray();
        for(int i = 0; i < 4; i++){
            rgba.put(1);
        }
        color.put("rgba", rgba);
        options.put("color", color);

        JSONObject backgroundColor = new JSONObject();
        JSONArray rgbaBC = new JSONArray();
        for(int i = 0; i < 4; i++){
            rgbaBC.put(0);
        }
        backgroundColor.put("rgba", rgbaBC);
        options.put("backgroundColor", backgroundColor);

        JSONObject position = new JSONObject();
        position.put("x", x);
        position.put("y", y);
        options.put("position", position);

        options.put("rotation", "0");
        options.put("maxWidth", maxWidth);
        options.put("flipHorizontally", false);
        options.put("flipVertically", false);

        jto.put("options", options);
        return jto;
    }

    private JSONObject getImageJson(String identifier, double x, double y, double dimX, double dimY) throws JSONException {
        JSONObject jto = new JSONObject();
        jto.put("type", "sticker");

        JSONObject options = new JSONObject();
        options.put("identifier", identifier);

        JSONObject dimensions = new JSONObject();
        dimensions.put("x", dimX);
        dimensions.put("y", dimY);
        options.put("dimensions", dimensions);


        JSONObject position = new JSONObject();
        position.put("x", x);
        position.put("y", y);
        options.put("position", position);

        options.put("flipHorizontally", false);
        options.put("flipVertically", false);

        JSONObject tintColor = new JSONObject();
        JSONArray rgba = new JSONArray();
        for(int i = 0; i < 4; i++){
            rgba.put(0.0);
        }
        tintColor.put("rgba", rgba);
        options.put("tintColor", tintColor);

        options.put("rotation", "0");

        jto.put("options", options);
        return jto;
    }

    public void writeJson(JSONObject jo, String jsonFileName){
        File file = new File(Environment.getExternalStorageDirectory(), jsonFileName);

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);

            pw.println(jo.toString());
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d("CUSTOM_JSON", jo.toString());
    }

    public void readNprintJson(String jsonFileName){
        File file = new File(Environment.getExternalStorageDirectory(), jsonFileName);
        StringBuilder text = new StringBuilder();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            // do exception handling
        } finally {
            try { br.close(); } catch (Exception e) { }
        }
        try {
            JSONObject job = new JSONObject(String.valueOf(text));
            Log.i("JSON_TEMPLATE_***", job.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
