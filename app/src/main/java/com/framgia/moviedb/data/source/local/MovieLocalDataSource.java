package com.framgia.moviedb.data.source.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import com.framgia.moviedb.data.model.Movie;
import com.framgia.moviedb.data.source.MovieDataSource;
import com.framgia.moviedb.service.movie.ApiListMovie;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuannt on 03/02/2017.
 * Project: moviedb_04
 * Package: com.framgia.moviedb.data.source.local
 */
public class MovieLocalDataSource implements MovieDataSource {
    private static MovieLocalDataSource sMovieLocalDataSource;
    private DataHelper mDataHelper;

    private MovieLocalDataSource(Context context) {
        mDataHelper = new DataHelper(context);
    }

    public static MovieLocalDataSource getInstance(Context context) {
        if (sMovieLocalDataSource == null)
            return new MovieLocalDataSource(context);
        return sMovieLocalDataSource;
    }

    @Override
    public void getMovies(@Nullable String type, @Nullable String page,
                          GetCallback getCallback) {
        List<Movie> movies = null;
        SQLiteDatabase db = mDataHelper.getReadableDatabase();
        String[] projection = new String[]{
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_ENTRY_ID,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_TITLE,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_POSTER,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_OVERVIEW,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_RATE_AVG,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_FAVORITE,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_RELEASE_DATE,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_TYPE
        };
        String selection = MoviePersistenceContract.MovieEntry.COLUMN_NAME_TYPE + " = ?";
        String[] selectionArgs = {type};
        String sortOrder = ApiListMovie.TOP_RATED.equals(type) ?
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_RATE_AVG + " DESC" : null;
        Cursor cursor = db.query(
            MoviePersistenceContract.MovieEntry.TABLE_NAME,
            projection, selection, selectionArgs, null, null, sortOrder);
        if (cursor != null && cursor.getCount() > 0) {
            movies = new ArrayList<>();
            while (cursor.moveToNext()) {
                movies.add(new Movie(cursor));
            }
        }
        if (cursor != null) cursor.close();
        if (movies == null) getCallback.onNotAvailable();
        else getCallback.onLoaded(movies);
    }

    @Override
    public void saveMovie(@Nullable String type, Movie data) {
        SQLiteDatabase db = mDataHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_ENTRY_ID, data.getId());
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_TITLE, data.getTitle());
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_TYPE,
                data.getType() != null ? data.getType() : Movie.TYPE_DEFAULT);
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_POSTER, data.getPoster());
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_OVERVIEW, data.getOverview());
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_RATE_AVG, data.getVoteAverage());
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_RELEASE_DATE,
                data.getReleaseDate());
            contentValues.put(
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_FAVORITE, data.isFavorite());
            int check = (int) db.insertWithOnConflict(
                MoviePersistenceContract.MovieEntry.TABLE_NAME, null, contentValues,
                SQLiteDatabase.CONFLICT_IGNORE);
            if (check == -1) {
                contentValues.remove(MoviePersistenceContract.MovieEntry.COLUMN_NAME_TYPE);
                String selection =
                    MoviePersistenceContract.MovieEntry.COLUMN_NAME_ENTRY_ID + " =?";
                String[] selectionArgs = {String.valueOf(data.getId())};
                db.updateWithOnConflict(MoviePersistenceContract.MovieEntry.TABLE_NAME,
                    contentValues, selection, selectionArgs, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            //Error in between database transaction
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    @Override
    public void deleteAllData(@Nullable String type) {
        SQLiteDatabase db = mDataHelper.getWritableDatabase();
        String whereClause =
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_TYPE + " LIKE ?" +
                " AND " + MoviePersistenceContract.MovieEntry.COLUMN_NAME_FAVORITE + "<>" +
                DataHelper.TRUE_VALUE;
        String[] whereClauseArgs = {type};
        db.delete(
            MoviePersistenceContract.MovieEntry.TABLE_NAME, whereClause, whereClauseArgs);
        db.close();
    }

    @Override
    public boolean getFavorite(Movie data) {
        SQLiteDatabase db = mDataHelper.getWritableDatabase();
        boolean isFavorite = false;
        String selection =
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?" + " AND " +
                MoviePersistenceContract.MovieEntry.COLUMN_NAME_FAVORITE + "=?";
        String[] selectionArgs = {
            String.valueOf(data.getId()), String.valueOf(DataHelper.TRUE_VALUE)};
        Cursor cursor = db.query(
            MoviePersistenceContract.MovieEntry.TABLE_NAME,
            null, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.getCount() > 0) isFavorite = true;
        if (cursor != null) cursor.close();
        db.close();
        return isFavorite;
    }

    @Override
    public void updateFavorite(@Nullable String type, Movie data) {
        saveMovie(type, data);
    }

    @Override
    public void loadMovies(String type, @Nullable String idOrQuery, String page,
                           GetCallback getCallback) {
    }

    @Override
    public void loadFavorite(GetCallback getCallback) {
        List<Movie> movies = null;
        SQLiteDatabase db = mDataHelper.getReadableDatabase();
        String[] projection = new String[]{
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_ENTRY_ID,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_TITLE,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_POSTER,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_OVERVIEW,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_RATE_AVG,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_FAVORITE,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_TYPE,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_RELEASE_DATE
        };
        String selection = MoviePersistenceContract.MovieEntry.COLUMN_NAME_FAVORITE + " = ?";
        String[] selectionArgs = {String.valueOf(DataHelper.TRUE_VALUE)};
        Cursor cursor = db.query(
            true,
            MoviePersistenceContract.MovieEntry.TABLE_NAME,
            projection, selection, selectionArgs,
            MoviePersistenceContract.MovieEntry.COLUMN_NAME_ENTRY_ID, null,
            null, null);
        if (cursor != null && cursor.getCount() > 0) {
            movies = new ArrayList<>();
            while (cursor.moveToNext()) {
                movies.add(new Movie(cursor));
            }
        }
        if (cursor != null) cursor.close();
        if (movies == null) getCallback.onNotAvailable();
        else getCallback.onLoaded(movies);
    }

    @Override
    public void loadDetalMovie(String movieId, LoadCallback loadCallback) {
        // not require for local
    }

    @Override
    public void loadMovieReview(String movieId, String page, GetCallback getCallback) {
        // not require for local
    }
}
