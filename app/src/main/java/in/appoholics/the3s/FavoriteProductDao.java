// FavoriteProductDao.java
package in.appoholics.the3s;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteProduct product);

    @Delete
    void delete(FavoriteProduct product);

    @Query("SELECT * FROM favorites_table ORDER BY name ASC")
    LiveData<List<FavoriteProduct>> getAllFavorites();

    @Query("SELECT * FROM favorites_table WHERE productId = :id")
    FavoriteProduct getFavoriteById(String id);
}