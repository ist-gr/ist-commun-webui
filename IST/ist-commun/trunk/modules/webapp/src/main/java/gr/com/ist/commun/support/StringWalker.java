package gr.com.ist.commun.support;

public class StringWalker {
    private String str;
    private int pos = 0;
    
    public StringWalker(String str) {
        this.str = str;
    }
    
    public String walk(int length) {
        String result = str.substring(pos, pos + length);
        pos += length;
        return result;
    }
    
}
