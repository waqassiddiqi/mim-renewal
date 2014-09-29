package mim.renewal.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

public class Util {
	public static String generateReferenceId() {
        //int min = 4097; // hex equivalant 1001
        //int max = 65534; // hex equivalant fffe

        Random r = new Random();
        //int decRand = r.nextInt(max - min + 1) + min;
        
        //String hexRand = Integer.toHexString(decRand);

        DateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmss");
        
        Calendar cal = Calendar.getInstance();
        String dateStr = dateFormat.format(cal.getTime());
        String refID = dateStr + r.nextInt(9999);
        
        return refID;
    }
}