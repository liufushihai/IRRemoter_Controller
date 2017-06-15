/*******************************************************************
 * Company:     Fuzhou Rockchip Electronics Co., Ltd
 * Description:   
 * @author:     fxw@rock-chips.com
 * Create at:   2014年5月7日 下午2:56:57  
 * 
 * Modification History:  
 * Date         Author      Version     Description  
 * ------------------------------------------------------------------  
 * 2014年5月7日      fxw         1.0         create
 *******************************************************************/

package com.example.mytest.irremoter_controller.utils;

import java.util.ArrayList;
import java.util.Locale;

public class StringUtils {
	private final static char[] mChars = "0123456789ABCDEF".toCharArray();  
    private final static String mHexStr = "0123456789ABCDEF";    
	/**
	 * Check that the given CharSequence is neither <code>null</code> nor of length 0.
	 * @return <code>true</code> if the obj is not null and obj.toString() has length
	 */
	public static boolean isEmptyObj(Object obj) {
		return obj == null || "".equals(obj);
	}

	/**
	 * Check that the given CharSequence is neither <code>null</code> nor of length 0.
	 * Note: Will return <code>true</code> for a CharSequence that purely consists of whitespace.
	 * <p><pre>
	 * StringUtils.hasLength(null) = false
	 * StringUtils.hasLength("") = false
	 * StringUtils.hasLength(" ") = true
	 * StringUtils.hasLength("Hello") = true
	 * </pre>
	 * @param str the CharSequence to check (may be <code>null</code>)
	 * @return <code>true</code> if the CharSequence is not null and has length
	 * @see #hasText(String)
	 */
	public static boolean hasLength(CharSequence str) {
		return (str != null && str.length() > 0);
	}

	/**
	 * Convert string to int
	 * @param str
	 * @param def
	 * @return
	 */
	public static int parseInt(String str, int def){
		int result = def;
		try{
			if(str!=null)
				result = Integer.parseInt(str);
		}catch(Exception e){
		}
		return result;
	}

	public static boolean getBooleanValue(String str,boolean def)
	{
		boolean result = def;
		try{
			if(str!=null)
				result = (Integer.parseInt(str)==0?false:true);
		}catch(Exception e){
		}
		return result;
	}

	public static String getStringValue(String str,String def)
	{
		if(str == null || str.length()==0)
		{
			return def;
		}
		return str;
	}

	public static String generateTime(long time) {
		int totalSeconds = (int) (time / 1000);
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
	}

	public static String getFileNameFromPath(String path) {
		String[] mstrs = path.split("/");	
		int length = mstrs.length;
		if(length == 0)
			return "";
		return mstrs[length - 1];
	}
	
	
	
	  /**  
     * 字符串转换成十六进制字符串 
     * @param str String 待转换的ASCII字符串 
     * @return String 每个Byte之间空格分隔，如: [61 6C 6B] 
     */    
    public static String str2HexStr(String str){    
        StringBuilder sb = new StringBuilder();  
        char[] bs = str.toCharArray();    
          
        for (int i = 0; i < bs.length; i++){    
            sb.append(mChars[(bs[i] & 0xFF) >> 4]);    
            sb.append(mChars[bs[i] & 0x0F]);  
            sb.append(' ');  
        }    
        return sb.toString().trim();    
    }  
      
    /**  
     * 十六进制字符串转换成 ASCII字符串 
     * @param str String Byte字符串 
     * @return String 对应的字符串 
     */    
    public static String hexStr2Str(String hexStr){    
        hexStr = hexStr.toString().trim().replace(" ", "").toUpperCase(Locale.US);  
        char[] hexs = hexStr.toCharArray();    
        char[] chars = new char[hexStr.length() / 2];    
        int iTmp = 0x00;;    
  
        for (int i = 0; i < chars.length; i++){    
            iTmp = mHexStr.indexOf(hexs[2 * i]) << 4;    
            iTmp |= mHexStr.indexOf(hexs[2 * i + 1]);    
            chars[i] = (char)iTmp;    
        }    
        return new String(chars);    
    }  

	
	public static ArrayList<String> spilt(String text, int length) {
		if (length < 1) {
			return null;
		}
		ArrayList<String> arrayList = new ArrayList<String>();
		if (text.length() > length) {
			arrayList.add(text.substring(0, length));
			arrayList.addAll(spilt(text.substring(length), length));

		} else {
			arrayList.add(text);
		}
		return arrayList;
	}
}
