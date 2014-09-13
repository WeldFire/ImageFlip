package rom.weldware.ImageFlip;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by WeldFire on 2/4/14.
 */
public class ImageStore {
    ArrayList<ImageUnit> imageList;
    final static String ImageStore_settingsID = "ImageStore_PATHS";

    public ImageStore(){
        imageList = new ArrayList<ImageUnit>();
    }

    public int size(){
        return imageList.size();
    }

    public ImageUnit get(){
        ImageUnit returned = imageList.get(0);
        imageList.remove(0);
        return returned;
    }

    public void clearImages(){
        this.deleteAllStoredFiles();
        this.imageList.clear();
    }

    public void addImage(Drawable image, String imageUrl, String imageFilepath){
        imageList.add(new ImageUnit(image, imageUrl, imageFilepath));
    }

    public void removeImage(String imageUrl){
        for(int i=0; i < imageList.size(); i++){
            ImageUnit iu = imageList.get(i);
            if(iu.imageURL.equals(imageUrl)){
                imageList.remove(i);
            }
        }
    }

    public void removeHost(String hostUrl){
        for(int i=0; i < imageList.size(); i++){
            ImageUnit iu = imageList.get(i);
            if(iu.baseURL.equals(hostUrl)){
                imageList.remove(i);
            }
        }
    }

    public void saveSettings(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ImageStore_settingsID, this.generateAllStoredPaths());
        editor.commit();
    }

    public void loadSettings(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String fullData = prefs.getString(ImageStore_settingsID, "");
        String indivPaths[] = fullData.split(",");

        for(String path:indivPaths){
            if(!path.isEmpty()) {
                String pathSplits[] = path.split("`");
                this.addImage(null, pathSplits[0], pathSplits[1]);
            }
        }
    }

    private void deleteAllStoredFiles(){
        for(ImageUnit unit: this.imageList){
            try {
                File selectedFile = new File(unit.imageURL);
                selectedFile.delete();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private String generateAllStoredPaths(){
        String returned = "";
        for(ImageUnit unit: this.imageList){
            returned += unit.imageURL + "`" + unit.imageFilepath + ",";
        }
        return returned;
    }

    public class ImageUnit {
        Drawable image;
        String imageURL;
        String imageFilepath;
        String baseURL;

        public ImageUnit(Drawable image, String imageURL, String imageFilepath){
            this.image = image;
            this.imageURL = imageURL;
            this.imageFilepath = imageFilepath;
        }
    }
}
