package in.appoholics.the3s;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

import java.util.ArrayList;

// This is the main activity of the revamped app.
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This will set the layout for the activity.
        setContentView(R.layout.activity_main);

        // This section handles the category buttons (3S and Verbose)
        Button threeSButton = findViewById(R.id.three_s_button);
        Button verboseButton = findViewById(R.id.verbose_button);

        threeSButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "3S category selected", Toast.LENGTH_SHORT).show();
            // Implement logic for 3S category selection
        });

        verboseButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Verbose category selected", Toast.LENGTH_SHORT).show();
            // Implement logic for Verbose category selection
        });

        // This section handles the creation of the grid items.
        // In a real app, you would dynamically populate this from data.
        // For this example, we'll assume a static grid.
        GridView categoriesGrid = findViewById(R.id.gridView);

        // Create an array of category data (Title and Image Resource ID)
        int[][] categories = {
                {R.string.table_clocks_text, R.drawable.ic_table_clock},
                {R.string.photo_frames_text, R.drawable.ic_photo_frame},
                {R.string.wall_clocks_text, R.drawable.ic_photo_frame},
                {R.string.trophyverse_text, R.drawable.ic_photo_frame},
        };

        categoriesGrid.setAdapter(new categoryAdapter(categories));

//            cardView.setOnClickListener(v -> {
//                // Handle click on a category card
//                String categoryName = getResources().getString(category[0]);
//                Toast.makeText(MainActivity.this, "Clicked " + categoryName, Toast.LENGTH_SHORT).show();
//            });



        // Handle the floating action button (WhatsApp) click
        View whatsappFab = findViewById(R.id.whatsapp_fab);
        whatsappFab.setOnClickListener(v -> {
            // Implement WhatsApp chat launch logic here
            Toast.makeText(MainActivity.this, "Opening WhatsApp chat...", Toast.LENGTH_SHORT).show();
        });


    }

    class categoryAdapter extends BaseAdapter {

        int[][] categories_data;

        public categoryAdapter(int[][] categories_data) {
            this.categories_data = categories_data;
        }

        @Override
        public int getCount() {
            return categories_data.length;
        }

        @Override
        public Object getItem(int position) {
            return categories_data[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflator.inflate(R.layout.item_category_card, parent, false);

            ImageView icon = convertView.findViewById(R.id.category_icon);
            TextView title = convertView.findViewById(R.id.category_title);
            title.setText(categories_data[position][0]);
            icon.setImageResource(categories_data[position][1]);

            return convertView;
        }
    }
}


