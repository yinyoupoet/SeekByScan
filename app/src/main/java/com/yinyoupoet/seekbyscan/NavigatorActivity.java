package com.yinyoupoet.seekbyscan;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

public class NavigatorActivity extends AppCompatActivity {

    private WebView webView;
    private long exitTime = 0;
    //起点和终点
    String destination = "";
    String start = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigator);

        Intent intent = getIntent();
        destination = intent.getStringExtra("position");

        webView = new WebView(this);

        //webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient(){
            //解决显示与跳转问题
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url == null) return false;

                Log.d("MY_URL", url);
                try{
                    if(url.startsWith("baidumap://")){
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }catch (Exception e){
                    return false;
                }
                webView.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //String s = "var qq = $('#fis_elm__4 div div div ul li:nth-child(3) a'); qq.click();$('#se-txt-start').val('长沙理工大学');$('#se-txt-end').val('湖南大学'); window.alert('js injection success');";
                String s = "$('#se-txt-start').val('我的位置');$('#se-txt-end').val('"+destination+"');";

                webView.loadUrl("javascript:" + s);
                super.onPageFinished(view, url);
            }
        });


        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);//设置js可以直接打开窗口，如window.open()，默认为false
        webView.getSettings().setJavaScriptEnabled(true);//是否允许执行js，默认为false。设置true时，会提醒可能造成XSS漏洞
        webView.getSettings().setSupportZoom(true);//是否可以缩放，默认true
        webView.getSettings().setBuiltInZoomControls(true);//是否显示缩放按钮，默认false
        webView.getSettings().setUseWideViewPort(true);//设置此属性，可任意比例缩放。大视图模式
        webView.getSettings().setLoadWithOverviewMode(true);//和setUseWideViewPort(true)一起解决网页自适应问题
        webView.getSettings().setAppCacheEnabled(true);//是否使用缓存
        webView.getSettings().setDomStorageEnabled(true);//DOM Storage
        int version = Build.VERSION.SDK_INT;
        if(version >= 21) {
            webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);//允许混合加载http与https
        }
// displayWebview.getSettings().setUserAgentString("User-Agent:Android");//设置用户代理，一般不用

        //webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("https://map.baidu.com/mobile/webapp/index/index/qt=cur&wd=%E5%8C%97%E4%BA%AC%E5%B8%82&from=maponline&tn=m01&ie=utf-8=utf-8/tab=line/?fromhash=1");
        setContentView(webView);
    }

    //region 重写物理返回键

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()){
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //endregion

}
