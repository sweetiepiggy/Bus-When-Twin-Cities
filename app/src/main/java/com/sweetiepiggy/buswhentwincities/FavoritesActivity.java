/*
    Copyright (C) 2019 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

    This file is part of Bus When? (Twin Cities).

    Bus When? (Twin Cities) is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    Bus When? (Twin Cities) is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bus When? (Twin Cities); if not, see <http://www.gnu.org/licenses/>.
*/

package com.sweetiepiggy.buswhentwincities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import static com.sweetiepiggy.buswhentwincities.DbAdapter.KEY_STOP_ID;

public class FavoritesActivity extends AppCompatActivity {
    private static final String SOURCE_URL = "https://github.com/sweetiepiggy/Bus-When-Twin-Cities";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final BottomNavigationView bnv = findViewById(R.id.bnv);
        bnv.getMenu().findItem(R.id.action_favorites).setChecked(true);
        bnv.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                    Intent intent;
                    switch (item.getItemId()) {
                    case R.id.action_search:
                        bnv.getMenu().findItem(R.id.action_favorites).setChecked(true);
                        intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.action_favorites:
                        return true;
                    }
                    return false;
                }
            }
            );

        RecyclerView favoritesRecyclerView = (RecyclerView) findViewById(R.id.favoritesRecyclerView);

        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecyclerView.
            addItemDecoration(new DividerItemDecoration(favoritesRecyclerView.getContext(),
                                                        DividerItemDecoration.VERTICAL));

        DbAdapter dbHelper = new DbAdapter();
        dbHelper.open(this);
        List<String> favoriteStopIds = new ArrayList<String>();
        Cursor c = dbHelper.fetchFavStops();
        int columnIndex = c.getColumnIndex(KEY_STOP_ID);
        while (c.moveToNext()) {
            favoriteStopIds.add(c.getString(columnIndex));
        }
        c.close();
        dbHelper.close();
        favoritesRecyclerView.setAdapter(new FavoriteStopIdsAdapter(getApplicationContext(),
                                                                    favoriteStopIds));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.action_about:
            intent = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(intent);
            return true;
        case R.id.action_source:
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(SOURCE_URL), "text/html");
            startActivity(Intent.createChooser(intent, null));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}

