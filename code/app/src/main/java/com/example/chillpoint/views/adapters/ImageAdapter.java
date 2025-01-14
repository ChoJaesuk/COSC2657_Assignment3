package com.example.chillpoint.views.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide; // Glide를 사용하여 이미지 로드 (Firebase URL 지원 추가)
import com.example.chillpoint.R;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Uri> imageUris; // 로컬 이미지 URI 리스트
    private ArrayList<String> uploadedImageUrls; // Firebase 업로드된 이미지 URL 리스트 (선택 사항)
    private boolean useFirebaseUrls; // Firebase URL 사용 여부 (선택 사항)
    private OnImageRemoveListener onImageRemoveListener;

    // 기본 생성자: 로컬 이미지 URI만 지원
    public ImageAdapter(Context context, ArrayList<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
        this.uploadedImageUrls = new ArrayList<>();
        this.useFirebaseUrls = false; // 기본적으로 Firebase URL 사용 안 함
    }

    // 확장 생성자: Firebase URL도 지원
    public ImageAdapter(Context context, ArrayList<Uri> imageUris, ArrayList<String> uploadedImageUrls) {
        this.context = context;
        this.imageUris = imageUris;
        this.uploadedImageUrls = uploadedImageUrls;
        this.useFirebaseUrls = true; // Firebase URL 사용
    }

    // Listener interface for removing images
    public interface OnImageRemoveListener {
        void onImageRemoved(int position);
    }

    public void setOnImageRemoveListener(OnImageRemoveListener listener) {
        this.onImageRemoveListener = listener;
    }

    @Override
    public int getCount() {
        if (useFirebaseUrls) {
            return uploadedImageUrls.size() + imageUris.size();
        }
        return imageUris.size();
    }

    @Override
    public Object getItem(int position) {
        if (useFirebaseUrls) {
            if (position < uploadedImageUrls.size()) {
                return uploadedImageUrls.get(position);
            }
            return imageUris.get(position - uploadedImageUrls.size());
        }
        return imageUris.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.imageView);
            holder.deleteButton = convertView.findViewById(R.id.deleteButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (useFirebaseUrls && position < uploadedImageUrls.size()) {
            // Firebase에서 업로드된 이미지 로드
            Glide.with(context)
                    .load(uploadedImageUrls.get(position)) // Firebase URL
                    .placeholder(R.drawable.placeholder_image) // 로딩 중 표시할 이미지
                    .into(holder.imageView);
        } else {
            // 로컬 URI 이미지 로드
            int localPosition = useFirebaseUrls ? position - uploadedImageUrls.size() : position;
            Uri imageUri = imageUris.get(localPosition);
            holder.imageView.setImageURI(imageUri);
        }

        // Handle delete button click
        holder.deleteButton.setOnClickListener(v -> {
            if (onImageRemoveListener != null) {
                onImageRemoveListener.onImageRemoved(position);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
        ImageButton deleteButton;
    }
}
