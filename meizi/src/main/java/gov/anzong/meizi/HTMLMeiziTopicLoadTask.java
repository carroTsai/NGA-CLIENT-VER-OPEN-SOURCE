package gov.anzong.meizi;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Locale;

import gov.anzong.meizi.MeiziTopicMData.ContentItemType;
import gov.anzong.meizi.MeiziTopicMData.TopicContentItem;
import gov.anzong.meizi.common.MeiziCookieManager;
import gov.anzong.meizi.utils.MeiziActivityUtils;
import gov.anzong.meizi.utils.MeiziStringUtils;

public class HTMLMeiziTopicLoadTask extends
        AsyncTask<String, Integer, MeiziTopicMData> {
    @SuppressWarnings("unused")
    private final static String TAG = HTMLMeiziTopicLoadTask.class
            .getSimpleName();
    @SuppressWarnings("unused")
    private static String rosmm = "http://www.rosmm.com";
    @SuppressWarnings("unused")
    private final Context context;
    final private OnMeiziTopicLoadFinishedListener notifier;

    public HTMLMeiziTopicLoadTask(Context context,
                                  OnMeiziTopicLoadFinishedListener notifier) {
        super();
        this.context = context;
        this.notifier = notifier;
    }

    @Override
    protected MeiziTopicMData doInBackground(String... params) {
        String url = params[0];
        String htmlString;
        htmlString = MeiziHttpUtil.getHtmlFormeizi(url, MeiziCookieManager.getInstance().getMeiziCookie());
        if (!TextUtils.isEmpty(htmlString)) {
            MeiziTopicMData resulTopicM = null;
            if (url.toLowerCase(Locale.US).indexOf("rosmm") > 0) {
                RosMMTopicDecoder mDecoder = new RosMMTopicDecoder();
                resulTopicM = mDecoder.decode(htmlString);
            } else {
                TopicDecoder mDecoder = new TopicDecoder();
                resulTopicM = mDecoder.decode(htmlString);
            }
            return resulTopicM;
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(MeiziTopicMData result) {
        MeiziActivityUtils.getInstance().dismiss();
        if (null != notifier)
            notifier.datafinishLoad(result);
        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        MeiziActivityUtils.getInstance().dismiss();
        super.onCancelled();
    }

    public static class TopicDecoder extends
            MeiziHtmlDecoderBase<MeiziTopicMData> {

        @Override
        public MeiziTopicMData decode(String html) {
            MeiziTopicMData resulTopicM = new MeiziTopicMData();

            Document document = Jsoup.parse(html);

            // get title string
            resulTopicM.title = document.select("div.post-title").select("h1")
                    .html().trim();
            if (!TextUtils.isEmpty(resulTopicM.title)) {
                resulTopicM.title = MeiziStringUtils.getStringBetween(
                        resulTopicM.title, 0, "/span>", "/*&*&--/");
            }
            // get post time
            resulTopicM.date = document.select("div.post-title")
                    .select("span.meta.date").html().trim();

            // get the main content, pictures and messages
            Elements contentElements = document.select("div.post-content")
                    .first().children();

            for (int i = 0; i < contentElements.size(); i++) {
                Element element = contentElements.get(i);
                String tagName = element.tagName();
                if (element.select("p").select("img").size() > 0) {
                    TopicContentItem item = new TopicContentItem();
                    item.type = ContentItemType.IMAGE;
                    item.imgUrl = element.select("p").select("img").attr("src");
                    if (!TextUtils.isEmpty(item.imgUrl)) {
                        resulTopicM.content.add(item);
                    }
                } else {
                    TopicContentItem item = new TopicContentItem();
                    item.type = ContentItemType.MSG;
                    String pContent = element.select("p").html();
                    item.msg = Html.fromHtml(pContent).toString().trim();

                    if (!TextUtils.isEmpty(item.msg)) {
                        resulTopicM.content.add(item);
                    }
                }
            }

            return resulTopicM;
        }
    }

    public static class RosMMTopicDecoder extends
            MeiziHtmlDecoderBase<MeiziTopicMData> {

        @Override
        public MeiziTopicMData decode(String html) {
            MeiziTopicMData resulTopicM = new MeiziTopicMData();

            Document document = Jsoup.parse(html);

            // get title string
            resulTopicM.title = document.select("div.title h1").html().trim();
            Elements contentElements = document.select("p#imgString").select("img");

            for (int i = 0; i < contentElements.size(); i++) {
                Element element = contentElements.get(i);
                TopicContentItem item = new TopicContentItem();
                item.type = ContentItemType.IMAGE;
                item.imgUrl = element.attr("src");

                if (!TextUtils.isEmpty(item.imgUrl)) {
                    resulTopicM.content.add(item);
                }
            }

            return resulTopicM;
        }
    }
}
