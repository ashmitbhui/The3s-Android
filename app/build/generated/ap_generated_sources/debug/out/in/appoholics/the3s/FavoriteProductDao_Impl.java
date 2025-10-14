package in.appoholics.the3s;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FavoriteProductDao_Impl implements FavoriteProductDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FavoriteProduct> __insertionAdapterOfFavoriteProduct;

  private final EntityDeletionOrUpdateAdapter<FavoriteProduct> __deletionAdapterOfFavoriteProduct;

  public FavoriteProductDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFavoriteProduct = new EntityInsertionAdapter<FavoriteProduct>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `favorites_table` (`productId`,`name`,`detail`,`price`,`imageUrl`,`category`,`brand`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final FavoriteProduct entity) {
        if (entity.getProductId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getProductId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getDetail() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDetail());
        }
        if (entity.getPrice() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPrice());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getImageUrl());
        }
        if (entity.getCategory() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getCategory());
        }
        if (entity.getBrand() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getBrand());
        }
      }
    };
    this.__deletionAdapterOfFavoriteProduct = new EntityDeletionOrUpdateAdapter<FavoriteProduct>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `favorites_table` WHERE `productId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final FavoriteProduct entity) {
        if (entity.getProductId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getProductId());
        }
      }
    };
  }

  @Override
  public void insert(final FavoriteProduct product) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfFavoriteProduct.insert(product);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final FavoriteProduct product) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfFavoriteProduct.handle(product);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<FavoriteProduct>> getAllFavorites() {
    final String _sql = "SELECT * FROM favorites_table ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"favorites_table"}, false, new Callable<List<FavoriteProduct>>() {
      @Override
      @Nullable
      public List<FavoriteProduct> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfProductId = CursorUtil.getColumnIndexOrThrow(_cursor, "productId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDetail = CursorUtil.getColumnIndexOrThrow(_cursor, "detail");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
          final List<FavoriteProduct> _result = new ArrayList<FavoriteProduct>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FavoriteProduct _item;
            final String _tmpProductId;
            if (_cursor.isNull(_cursorIndexOfProductId)) {
              _tmpProductId = null;
            } else {
              _tmpProductId = _cursor.getString(_cursorIndexOfProductId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpDetail;
            if (_cursor.isNull(_cursorIndexOfDetail)) {
              _tmpDetail = null;
            } else {
              _tmpDetail = _cursor.getString(_cursorIndexOfDetail);
            }
            final String _tmpPrice;
            if (_cursor.isNull(_cursorIndexOfPrice)) {
              _tmpPrice = null;
            } else {
              _tmpPrice = _cursor.getString(_cursorIndexOfPrice);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpBrand;
            if (_cursor.isNull(_cursorIndexOfBrand)) {
              _tmpBrand = null;
            } else {
              _tmpBrand = _cursor.getString(_cursorIndexOfBrand);
            }
            _item = new FavoriteProduct(_tmpProductId,_tmpName,_tmpDetail,_tmpPrice,_tmpImageUrl,_tmpCategory,_tmpBrand);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public FavoriteProduct getFavoriteById(final String id) {
    final String _sql = "SELECT * FROM favorites_table WHERE productId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfProductId = CursorUtil.getColumnIndexOrThrow(_cursor, "productId");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfDetail = CursorUtil.getColumnIndexOrThrow(_cursor, "detail");
      final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
      final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
      final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
      final int _cursorIndexOfBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "brand");
      final FavoriteProduct _result;
      if (_cursor.moveToFirst()) {
        final String _tmpProductId;
        if (_cursor.isNull(_cursorIndexOfProductId)) {
          _tmpProductId = null;
        } else {
          _tmpProductId = _cursor.getString(_cursorIndexOfProductId);
        }
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        final String _tmpDetail;
        if (_cursor.isNull(_cursorIndexOfDetail)) {
          _tmpDetail = null;
        } else {
          _tmpDetail = _cursor.getString(_cursorIndexOfDetail);
        }
        final String _tmpPrice;
        if (_cursor.isNull(_cursorIndexOfPrice)) {
          _tmpPrice = null;
        } else {
          _tmpPrice = _cursor.getString(_cursorIndexOfPrice);
        }
        final String _tmpImageUrl;
        if (_cursor.isNull(_cursorIndexOfImageUrl)) {
          _tmpImageUrl = null;
        } else {
          _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
        }
        final String _tmpCategory;
        if (_cursor.isNull(_cursorIndexOfCategory)) {
          _tmpCategory = null;
        } else {
          _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
        }
        final String _tmpBrand;
        if (_cursor.isNull(_cursorIndexOfBrand)) {
          _tmpBrand = null;
        } else {
          _tmpBrand = _cursor.getString(_cursorIndexOfBrand);
        }
        _result = new FavoriteProduct(_tmpProductId,_tmpName,_tmpDetail,_tmpPrice,_tmpImageUrl,_tmpCategory,_tmpBrand);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
