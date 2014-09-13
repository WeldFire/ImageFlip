package rom.weldware.ImageFlip;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by WeldFire on 7/23/13.
 */
public class SerializableArrayList extends ArrayList<String> implements Serializable {
    private transient String  filename;
    private transient Context context;

    public SerializableArrayList(Context context, String inputFilename){
        this.context = context;
        this.filename = inputFilename;
    }

    public void save(){
        FileOutputStream outputStream;

        try {
            outputStream = this.context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oopsStream = new ObjectOutputStream(outputStream);
            oopsStream.writeObject(this);
            oopsStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(){
        FileInputStream inputStream = null;
        ObjectInputStream oipsStream = null;
        try {
            inputStream = this.context.openFileInput(filename);
            oipsStream = new ObjectInputStream(inputStream);

            SerializableArrayList readObject = (SerializableArrayList)oipsStream.readObject();

            this.clear();
            this.addAll(readObject);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (oipsStream != null) {
                try {
                    oipsStream.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
