package com.huanchengfly.tieba.post.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huanchengfly.tieba.post.R;
import com.huanchengfly.tieba.post.activities.BaseActivity;
import com.huanchengfly.tieba.post.activities.ReplyActivity;
import com.huanchengfly.tieba.post.adapters.base.BaseSingleTypeAdapter;
import com.huanchengfly.tieba.post.api.TiebaApi;
import com.huanchengfly.tieba.post.api.models.CommonResponse;
import com.huanchengfly.tieba.post.api.models.SubFloorListBean;
import com.huanchengfly.tieba.post.api.models.ThreadContentBean;
import com.huanchengfly.tieba.post.components.LinkMovementClickMethod;
import com.huanchengfly.tieba.post.components.LinkTouchMovementMethod;
import com.huanchengfly.tieba.post.components.MyViewHolder;
import com.huanchengfly.tieba.post.components.spans.MyURLSpan;
import com.huanchengfly.tieba.post.components.spans.MyUserSpan;
import com.huanchengfly.tieba.post.fragments.ConfirmDialogFragment;
import com.huanchengfly.tieba.post.fragments.MenuDialogFragment;
import com.huanchengfly.tieba.post.models.PhotoViewBean;
import com.huanchengfly.tieba.post.models.ReplyInfoBean;
import com.huanchengfly.tieba.post.plugins.PluginManager;
import com.huanchengfly.tieba.post.ui.widgets.MyLinearLayout;
import com.huanchengfly.tieba.post.ui.widgets.VoicePlayerView;
import com.huanchengfly.tieba.post.ui.widgets.theme.TintMySpannableTextView;
import com.huanchengfly.tieba.post.ui.widgets.theme.TintTextView;
import com.huanchengfly.tieba.post.utils.AccountUtil;
import com.huanchengfly.tieba.post.utils.BilibiliUtil;
import com.huanchengfly.tieba.post.utils.DateTimeUtils;
import com.huanchengfly.tieba.post.utils.EmoticonManager;
import com.huanchengfly.tieba.post.utils.EmoticonUtil;
import com.huanchengfly.tieba.post.utils.ImageUtil;
import com.huanchengfly.tieba.post.utils.NavigationHelper;
import com.huanchengfly.tieba.post.utils.StringUtil;
import com.huanchengfly.tieba.post.utils.ThemeUtil;
import com.huanchengfly.tieba.post.utils.TiebaUtil;
import com.huanchengfly.tieba.post.utils.Util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecyclerFloorAdapter extends BaseSingleTypeAdapter<SubFloorListBean.PostInfo> {
    public static final String TAG = "RecyclerFloorAdapter";
    private static final int TEXT_VIEW_TYPE_CONTENT = 0;
    private final LinearLayout.LayoutParams defaultLayoutParams;
    private final Float maxWidth;
    private SubFloorListBean dataBean;

    public RecyclerFloorAdapter(Context context) {
        super(context, null);
        setOnItemClickListener((viewHolder, postInfo, position) -> {
            if (!AccountUtil.isLoggedIn()) return;
            int floor = Integer.parseInt(dataBean.getPost().getFloor());
            int pn = floor - (floor % 30);
            ThreadContentBean.UserInfoBean userInfoBean = postInfo.getAuthor();
            getContext().startActivity(new Intent(getContext(), ReplyActivity.class)
                    .putExtra("data", new ReplyInfoBean(dataBean.getThread().getId(),
                            dataBean.getForum().getId(),
                            dataBean.getForum().getName(),
                            dataBean.getAnti().getTbs(),
                            dataBean.getPost().getId(),
                            postInfo.getId(),
                            dataBean.getPost().getFloor(),
                            userInfoBean != null ? userInfoBean.getNameShow() : "",
                            AccountUtil.getLoginInfo().getNameShow()).setPn(String.valueOf(pn)).toString()));
        });
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        maxWidth = (float) dm.widthPixels;
        defaultLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        defaultLayoutParams.setMargins(0, 8, 0, 8);
    }

    public void setData(SubFloorListBean data) {
        dataBean = data;
        data.getSubPostList().add(0, data.getPost());
        setData(data.getSubPostList());
    }

    public void addData(SubFloorListBean data) {
        dataBean = data;
        insert(data.getSubPostList());
    }

    private void showMenu(SubFloorListBean.PostInfo postInfo, int position) {
        ThreadContentBean.UserInfoBean userInfoBean = postInfo.getAuthor();
        MenuDialogFragment.newInstance(R.menu.menu_thread_item, null)
                .setOnNavigationItemSelectedListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_reply:
                            int floor = Integer.parseInt(dataBean.getPost().getFloor());
                            int pn = floor - (floor % 30);
                            String replyData = new ReplyInfoBean(dataBean.getThread().getId(),
                                    dataBean.getForum().getId(),
                                    dataBean.getForum().getName(),
                                    dataBean.getAnti().getTbs(),
                                    dataBean.getPost().getId(),
                                    postInfo.getId(),
                                    dataBean.getPost().getFloor(),
                                    userInfoBean != null ? userInfoBean.getNameShow() : "",
                                    AccountUtil.getLoginInfo().getNameShow()).setPn(String.valueOf(pn)).toString();
                            Log.i(TAG, "convert: " + replyData);
                            getContext().startActivity(new Intent(getContext(), ReplyActivity.class)
                                    .putExtra("data", replyData));
                            return true;
                        case R.id.menu_report:
                            TiebaUtil.reportPost(getContext(), postInfo.getId());
                            return true;
                        case R.id.menu_copy:
                            StringBuilder stringBuilder = new StringBuilder();
                            for (ThreadContentBean.ContentBean contentBean : postInfo.getContent()) {
                                switch (contentBean.getType()) {
                                    case "2":
                                        contentBean.setText("#(" + contentBean.getC() + ")");
                                        EmoticonManager.INSTANCE.registerEmoticon(contentBean.getText(), contentBean.getC());
                                        break;
                                    case "3":
                                    case "20":
                                        contentBean.setText("[图片]\n");
                                        break;
                                    case "10":
                                        contentBean.setText("[语音]\n");
                                        break;
                                }
                                if (contentBean.getText() != null) {
                                    stringBuilder.append(contentBean.getText());
                                }
                            }
                            Util.showCopyDialog((BaseActivity) getContext(), stringBuilder.toString(), postInfo.getId());
                            return true;
                        case R.id.menu_delete:
                            if (TextUtils.equals(AccountUtil.getLoginInfo().getUid(), postInfo.getAuthor().getId())) {
                                ConfirmDialogFragment.newInstance(getContext().getString(R.string.title_dialog_del_post))
                                        .setOnConfirmListener(() -> {
                                            TiebaApi.getInstance()
                                                    .delPost(dataBean.getForum().getId(), dataBean.getForum().getName(), dataBean.getThread().getId(), postInfo.getId(), dataBean.getAnti().getTbs(), true, true)
                                                    .enqueue(new Callback<CommonResponse>() {
                                                        @Override
                                                        public void onResponse(@NotNull Call<CommonResponse> call, @NotNull Response<CommonResponse> response) {
                                                            Toast.makeText(getContext(), R.string.toast_success, Toast.LENGTH_SHORT).show();
                                                            remove(position);
                                                        }

                                                        @Override
                                                        public void onFailure(@NotNull Call<CommonResponse> call, @NotNull Throwable t) {
                                                            Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        })
                                        .show(((BaseActivity) getContext()).getSupportFragmentManager(), postInfo.getId() + "_Confirm");
                            }
                            return true;
                    }
                    return PluginManager.INSTANCE.performPluginMenuClick(PluginManager.MENU_SUB_POST_ITEM, item.getItemId(), postInfo);
                })
                .setInitMenuCallback(menu -> {
                    PluginManager.INSTANCE.initPluginMenu(menu, PluginManager.MENU_SUB_POST_ITEM);
                    if (AccountUtil.isLoggedIn() && TextUtils.equals(AccountUtil.getLoginInfo().getUid(), postInfo.getAuthor().getId())) {
                        menu.findItem(R.id.menu_delete).setVisible(true);
                    }
                })
                .show(((BaseActivity) getContext()).getSupportFragmentManager(), postInfo.getId() + "_Menu");
    }

    @Override
    protected void convert(MyViewHolder holder, SubFloorListBean.PostInfo data, int position) {
        ThreadContentBean.UserInfoBean userInfoBean = data.getAuthor();
        if (dataBean != null && dataBean.getThread() != null && dataBean.getThread().getAuthor() != null && userInfoBean != null && userInfoBean.getId() != null && userInfoBean.getId().equals(dataBean.getThread().getAuthor().getId())) {
            holder.setVisibility(R.id.thread_list_item_user_lz_tip, View.VISIBLE);
        } else {
            holder.setVisibility(R.id.thread_list_item_user_lz_tip, View.GONE);
        }
        holder.itemView.setOnLongClickListener(v -> {
            showMenu(data, position);
            return true;
        });
        holder.setText(R.id.thread_list_item_user_name, userInfoBean == null ? "" : StringUtil.getUsernameString(getContext(), userInfoBean.getName(), userInfoBean.getNameShow()));
        holder.setText(R.id.thread_list_item_user_time, DateTimeUtils.getRelativeTimeString(getContext(), data.getTime()));
        if (userInfoBean != null) {
            String levelId = userInfoBean.getLevelId() == null || TextUtils.isEmpty(userInfoBean.getLevelId()) ? "?" : userInfoBean.getLevelId();
            ThemeUtil.setChipThemeByLevel(levelId,
                    holder.getView(R.id.thread_list_item_user_status),
                    holder.getView(R.id.thread_list_item_user_level),
                    holder.getView(R.id.thread_list_item_user_lz_tip));
            holder.setText(R.id.thread_list_item_user_level, levelId);
            holder.setOnClickListener(R.id.thread_list_item_user_avatar, view -> {
                NavigationHelper.toUserSpaceWithAnim(getContext(), userInfoBean.getId(), StringUtil.getAvatarUrl(userInfoBean.getPortrait()), view);
            });
            ImageUtil.load(holder.getView(R.id.thread_list_item_user_avatar), ImageUtil.LOAD_TYPE_AVATAR, userInfoBean.getPortrait());
        }
        initContentView(holder, data);
    }

    @Override
    protected int getItemLayoutId() {
        return R.layout.item_thread_list;
    }

    private boolean appendTextToLastTextView(List<View> views, CharSequence newContent) {
        if (views.size() > 0) {
            View lastView = views.get(views.size() - 1);
            if (lastView instanceof TextView) {
                TextView lastTextView = (TextView) lastView;
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(lastTextView.getText());
                spannableStringBuilder.append(newContent);
                setText(lastTextView, spannableStringBuilder);
                return false;
            }
        }
        return true;
    }

    private TextView createTextView(int type) {
        TextView textView;
        if (type == TEXT_VIEW_TYPE_CONTENT) {
            TintMySpannableTextView mySpannableTextView = new TintMySpannableTextView(getContext());
            mySpannableTextView.setTintResId(R.color.default_color_text);
            mySpannableTextView.setLinkTouchMovementMethod(LinkTouchMovementMethod.getInstance());
            textView = mySpannableTextView;
        } else {
            TintTextView tintTextView = new TintTextView(getContext());
            tintTextView.setTintResId(R.color.default_color_text);
            tintTextView.setMovementMethod(LinkMovementClickMethod.getInstance());
            textView = tintTextView;
        }
        textView.setFocusable(false);
        textView.setClickable(false);
        textView.setLongClickable(false);
        textView.setTextIsSelectable(false);
        textView.setOnClickListener(null);
        textView.setOnLongClickListener(null);
        textView.setLetterSpacing(0.02F);
        if (type == TEXT_VIEW_TYPE_CONTENT) {
            textView.setTextSize(16);
        }
        return textView;
    }

    private void setText(TextView textView, CharSequence content) {
        content = BilibiliUtil.replaceVideoNumberSpan(getContext(), content);
        content = StringUtil.getEmoticonContent(textView, content, EmoticonUtil.EMOTICON_ALL_TYPE);
        textView.setText(content);
    }

    private LinearLayout.LayoutParams getLayoutParams(ThreadContentBean.ContentBean contentBean) {
        if (!contentBean.getType().equals("3") && !contentBean.getType().equals("5")) {
            return defaultLayoutParams;
        }
        float widthFloat, heightFloat;
        if (contentBean.getType().equals("3") || contentBean.getType().equals("20")) {
            String[] strings = contentBean.getBsize().split(",");
            widthFloat = Float.parseFloat(strings[0]);
            heightFloat = Float.parseFloat(strings[1]);
            heightFloat *= this.maxWidth / widthFloat;
            widthFloat = this.maxWidth;
        } else {
            float width = Float.parseFloat(contentBean.getWidth());
            widthFloat = this.maxWidth;
            heightFloat = Float.parseFloat(contentBean.getHeight());
            heightFloat *= widthFloat / width;
        }
        int width = Math.round(widthFloat);
        int height = Math.round(heightFloat);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        layoutParams.setMargins(0, 8, 0, 8);
        return layoutParams;
    }

    private CharSequence getLinkContent(CharSequence newContent, String url) {
        return getLinkContent("", newContent, url);
    }

    private CharSequence getLinkContent(CharSequence oldContent, CharSequence newContent, String url) {
        int start = oldContent.length();
        int end = start + newContent.length();
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(oldContent);
        spannableStringBuilder.append(newContent);
        spannableStringBuilder.setSpan(new MyURLSpan(getContext(), url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableStringBuilder;
    }

    private boolean appendLinkToLastTextView(List<View> views, CharSequence newContent, String url) {
        if (views.size() > 0) {
            View lastView = views.get(views.size() - 1);
            if (lastView instanceof TextView) {
                TextView lastTextView = (TextView) lastView;
                setText(lastTextView, getLinkContent(lastTextView.getText(), newContent, url));
                return false;
            }
        }
        return true;
    }

    private boolean canLoadGlide() {
        if (getContext() instanceof Activity) {
            return !((Activity) getContext()).isDestroyed();
        }
        return false;
    }

    private boolean appendUserToLastTextView(List<View> views, CharSequence newContent, String uid) {
        if (views.size() > 0) {
            View lastView = views.get(views.size() - 1);
            if (lastView instanceof TextView) {
                TextView lastTextView = (TextView) lastView;
                setText(lastTextView, getUserContent(lastTextView.getText(), newContent, uid));
                return false;
            }
        }
        return true;
    }

    private CharSequence getUserContent(CharSequence newContent, String uid) {
        return getUserContent("", newContent, uid);
    }

    private CharSequence getUserContent(CharSequence oldContent, CharSequence newContent, String uid) {
        int start = oldContent.length();
        int end = start + newContent.length();
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(oldContent);
        spannableStringBuilder.append(newContent);
        spannableStringBuilder.setSpan(new MyUserSpan(getContext(), uid), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableStringBuilder;
    }

    private List<View> getContentViews(SubFloorListBean.PostInfo postListItemBean) {
        List<View> views = new ArrayList<>();
        for (ThreadContentBean.ContentBean contentBean : postListItemBean.getContent()) {
            switch (contentBean.getType()) {
                case "0":
                case "9": {
                    if (appendTextToLastTextView(views, contentBean.getText())) {
                        TextView textView = createTextView(TEXT_VIEW_TYPE_CONTENT);
                        textView.setLayoutParams(getLayoutParams(contentBean));
                        setText(textView, contentBean.getText());
                        views.add(textView);
                    }
                }
                break;
                case "1":
                    if (appendLinkToLastTextView(views, contentBean.getText(), contentBean.getLink())) {
                        TextView textView = createTextView(TEXT_VIEW_TYPE_CONTENT);
                        textView.setLayoutParams(getLayoutParams(contentBean));
                        setText(textView, getLinkContent(contentBean.getText(), contentBean.getLink()));
                        views.add(textView);
                    }
                    break;
                case "2":
                    String emojiText = "#(" + contentBean.getC() + ")";
                    EmoticonManager.INSTANCE.registerEmoticon(contentBean.getText(), contentBean.getC());
                    if (appendTextToLastTextView(views, emojiText)) {
                        TextView textView = createTextView(TEXT_VIEW_TYPE_CONTENT);
                        textView.setLayoutParams(getLayoutParams(contentBean));
                        setText(textView, emojiText);
                        views.add(textView);
                    }
                    break;
                case "3":
                    ImageView imageView = new ImageView(getContext());
                    imageView.setLayoutParams(getLayoutParams(contentBean));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    ImageUtil.load(imageView, ImageUtil.LOAD_TYPE_SMALL_PIC, contentBean.getSrc());
                    List<PhotoViewBean> photoViewBeans = new ArrayList<>();
                    photoViewBeans.add(new PhotoViewBean(ImageUtil.getNonNullString(contentBean.getSrc(), contentBean.getOriginSrc()),
                            ImageUtil.getNonNullString(contentBean.getOriginSrc(), contentBean.getSrc()),
                            "1".equals(contentBean.isLongPic())));
                    ImageUtil.initImageView(imageView, photoViewBeans, 0);
                    views.add(imageView);
                    break;
                case "4":
                    if (appendUserToLastTextView(views, contentBean.getText(), contentBean.getUid())) {
                        TextView textView = createTextView(TEXT_VIEW_TYPE_CONTENT);
                        textView.setLayoutParams(getLayoutParams(contentBean));
                        setText(textView, getUserContent(contentBean.getText(), contentBean.getUid()));
                        views.add(textView);
                    }
                    break;
                case "10":
                    String voiceUrl = "https://tiebac.baidu.com/c/p/voice?voice_md5=" + contentBean.getVoiceMD5() + "&play_from=pb_voice_play";
                    VoicePlayerView voicePlayerView = new VoicePlayerView(getContext());
                    voicePlayerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    //voicePlayerView.setMini(false);
                    voicePlayerView.setDuration(Integer.parseInt(contentBean.getDuringTime()));
                    voicePlayerView.setUrl(voiceUrl);
                    views.add(voicePlayerView);
                    break;
            }
        }
        return views;
    }

    private void initContentView(MyViewHolder viewHolder, SubFloorListBean.PostInfo postListItemBean) {
        MyLinearLayout myLinearLayout = viewHolder.getView(R.id.thread_list_item_content_content);
        myLinearLayout.removeAllViews();
        myLinearLayout.addViews(getContentViews(postListItemBean));
    }
}