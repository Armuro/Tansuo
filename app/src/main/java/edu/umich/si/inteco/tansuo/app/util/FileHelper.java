package edu.umich.si.inteco.tansuo.app.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import edu.umich.si.inteco.tansuo.app.GlobalNames;
import edu.umich.si.inteco.tansuo.app.contextmanager.ActivityRecognitionService;
import edu.umich.si.inteco.tansuo.app.contextmanager.TransportationModeDetector;
import edu.umich.si.inteco.tansuo.app.model.record.ActivityRecord;

public class FileHelper {

	/** Tag for logging. */
    private static final String LOG_TAG = "FileHelper";
    
    private static Context mContext;
    
    public FileHelper(Context context) {
		mContext = context;
	}


    public static File getPackageDirectory() {

        return new File (Environment.getExternalStorageDirectory() + GlobalNames.PACKAGE_DIRECTORY_PATH);
    }

    public static File getLogDirectory() {

        return new File (Environment.getExternalStorageDirectory() + GlobalNames.PACKAGE_DIRECTORY_PATH + LogManager.LOG_DIRECTORY_PATH);
    }

	public static void writeStringToFile(String directory_name, String filename, String content){
    	
    	if(isExternalStorageWritable()){
			try{
				File PackageDirectory = new File(Environment.getExternalStorageDirectory() + GlobalNames.PACKAGE_DIRECTORY_PATH);
				
				//check whether the project diectory exists
				if(!PackageDirectory.exists()){
					PackageDirectory.mkdir();
				}
				
				File directory = new File (Environment.getExternalStorageDirectory() + GlobalNames.PACKAGE_DIRECTORY_PATH + directory_name);

				//check whether the directory exists
				if(!directory.exists()){
					directory.mkdir();
				}
				
				String pathfilename = Environment.getExternalStorageDirectory()+ GlobalNames.PACKAGE_DIRECTORY_PATH+directory_name+ filename;
				//Log.d(LOG_TAG, "[writeStringToFile] the file name is " + pathfilename + " the content is " + content);
				File file = new File(pathfilename);
				FileWriter filewriter = new FileWriter(file, true);
				BufferedWriter out = new BufferedWriter(filewriter);
				out.write(content);
				out.close();
			}catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage()+"");
				
			}
		}
    	
    }
    
    
    /**
     * get a string file name from the asset folder
     * @param filename
     * @return
     */
    public static String loadFileFromAsset(String filename) {
    	
    	
        String str = null;
        try {

            InputStream is = mContext.getAssets().open(filename);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            str = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        
        return str;

    }


    public static void readFilesFromInDirectory(String directoryPath) {

        //Get the text file
        File file[] = new File(directoryPath).listFiles();

        int i = 0;
        String filePath="";
        while(i!=file.length){
            filePath = file[i].getAbsolutePath();
            Log.d(LOG_TAG, "[readFilesFromInDirectory] " + i+" " +   filePath);
        }

    }



    /**
     * this function will read files to test the TransportationMode Detection
     */
    public static void readTestFile() {

        //testing postfiles


        String string = loadFileFromAsset("testData3.txt");
        String[] lines = string.split(System.getProperty("line.separator"));
        for (int i=0; i<lines.length; i++) {

            long time = Long.parseLong(lines[i].split("\t")[0]);
            String activitiesStr =  lines[i].split("\t")[1];

            String [] activities = activitiesStr.split(";;");
            //Log.d(LOG_TAG, "[readTestFile] readline " + lines[i]);
            List<DetectedActivity> activityList = new ArrayList<DetectedActivity>();

            for (int j=0; j<activities.length; j++){
                String activityStr = activities[j].split(":")[0];
                int confidence = Integer.parseInt(activities[j].split(":")[1]);

                DetectedActivity activity= new DetectedActivity(ActivityRecognitionService.getActivityTypeFromName(activityStr),confidence);
                activityList.add(activity);
                //  Log.d(LOG_TAG, "[readTestFile] activity " + activity + " : " + confidence);
            }

            ActivityRecord record = new ActivityRecord();
            record.setProbableActivities(activityList);
            record.setTimestamp(time);
            //  Log.d(LOG_TAG, "[readTestFile] readline " + lines[i]);

            //also add to the transportationModeDetector
            TransportationModeDetector.addActivityRecord(record);

        }


        // TransportationModeDetector.test();
    }


    /* Checks if external storage is available for read and write */
	private static boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}


    public static void readFilesFromNarrative() {

        //Get the text file
        File file[] = new File(Environment.getExternalStorageDirectory() + GlobalNames.PACKAGE_DIRECTORY_NARRATIVE_PATH).listFiles();

        recursiveFileFind(file);
    }


    public static void recursiveFileFind(File[] file1){
        int i = 0;
        String filePath="";
        if(file1!=null){
            while(i!=file1.length){
                filePath = file1[i].getAbsolutePath();
                if(file1[i].isDirectory()){
                    File file[] = file1[i].listFiles();
                    recursiveFileFind(file);
                }
                i++;
                Log.d(LOG_TAG, "[recursiveFileFind] " + i+" " +   filePath);
            }
        }
    }


}
