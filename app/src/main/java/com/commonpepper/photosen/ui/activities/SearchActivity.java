package com.commonpepper.photosen.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.commonpepper.photosen.R;
import com.commonpepper.photosen.ui.adapters.MyPagerAdapter;
import com.commonpepper.photosen.ui.fragments.SearchListFragment;
import com.commonpepper.photosen.ui.viewmodels.SearchActivityViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

public class SearchActivity extends AbstractNavActivity {
    public static final String TAG_SEARCHTAG = "search_tag";
    private static final String TAG_RECREATED = "rotated_tag";

    private EditText searchText;
    private ChipGroup chipGroup;
    private Chip firstChip;
    private EditText inputTag;
    private ViewPager viewPager;
    private MyPagerAdapter mPagerAdapter;
    private TabLayout tabLayout;
    private SearchActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        searchText = findViewById(R.id.search_edit_text);
        Toolbar toolbar = findViewById(R.id.search_toolbar);
        chipGroup = findViewById(R.id.search_chip_group);
        firstChip = findViewById(R.id.search_first_chip);
        inputTag = findViewById(R.id.tag_input);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        navigationView.getMenu().findItem(R.id.drawer_search).setCheckable(true);
        navigationView.getMenu().findItem(R.id.drawer_search).setChecked(true);
        navigationView.setNavigationItemSelectedListener(this);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        Intent intent = getIntent();
        String firstTag = intent.getStringExtra(TAG_SEARCHTAG);
        boolean recreated = false;
        if (savedInstanceState != null) recreated = savedInstanceState.getBoolean(TAG_RECREATED);
        viewModel = ViewModelProviders.of(this).get(SearchActivityViewModel.class);

        if (viewModel.getTags().size() > 0 || firstTag == null || recreated) {
            for (String tag : viewModel.getTags()) {
                addNewChip(tag);
            }
            chipGroup.removeView(firstChip);
            if (!recreated) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            } else {
                doSearch();
            }
        } else {
            viewModel.getTags().add(firstTag);
            firstChip.setText(firstTag);
            firstChip.setOnCloseIconClickListener(onChipClickListener);
            doSearch();
        }

        searchText.setOnEditorActionListener((v, actionId, event) -> {
            viewModel.setQueue(searchText.getText().toString());
            doSearch();
            return true;
        });

        inputTag.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null && event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    String newTag = inputTag.getText().toString();
                    if (newTag.length() > 0 && !viewModel.getTags().contains(newTag)) {
                        addNewChip(newTag);
                        viewModel.getTags().add(newTag);
                        inputTag.setText("");
                        doSearch();
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void addNewChip(String newTag) {
        Chip chip = new Chip(this);
        chip.setText(newTag);
        chip.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(onChipClickListener);
        chipGroup.addView(chip);
    }

    private View.OnClickListener onChipClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View x) {
            Chip chip = (Chip) x;
            chipGroup.removeView(chip);
            viewModel.getTags().remove(chip.getText().toString());
            SearchActivity.this.doSearch();
        }
    };

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (searchText.getText().toString().length() > 0) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    private void doSearch() {
        String query = viewModel.getQueue();
        if (query != null && query.length() == 0) query = null;

        String tagsExtra = viewModel.tagsToString();
        if (query != null || tagsExtra != null) {
            tabLayout.setVisibility(View.VISIBLE);
            SearchListFragment fragmentRelevant = SearchListFragment.newInstance(query, tagsExtra, "relevance");
            SearchListFragment fragmentMostViewed = SearchListFragment.newInstance(query, tagsExtra, "interestingness-desc");
            SearchListFragment fragmentLatest = SearchListFragment.newInstance(query, tagsExtra, "date-posted-desc");
            mPagerAdapter.clear();
            mPagerAdapter.addFragment(fragmentRelevant, getString(R.string.relevant));
            mPagerAdapter.addFragment(fragmentMostViewed, getString(R.string.most_viewed));
            mPagerAdapter.addFragment(fragmentLatest, getString(R.string.latest));

            View view = this.getCurrentFocus();
            if (view == null) view = new View(this);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(TAG_RECREATED, true);
        super.onSaveInstanceState(savedInstanceState);
    }

}
