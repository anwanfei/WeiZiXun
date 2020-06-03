package com.anfly.weizixun.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseFragment extends Fragment {

    private Unbinder bind;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);

        bind = ButterKnife.bind(this, view);

        initMvp();
        initView();
        initData();
        initListener();
        return view;
    }

    public void initMvp() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bind.unbind();
    }

    private void initListener() {

    }

    private void initData() {

    }

    private void initView() {

    }

    protected abstract int getLayoutId();
}
