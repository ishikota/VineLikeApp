package com.ikota.vinelikeapp.util;


import android.content.Context;

import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

public class VineHelper {

    public static boolean combineVideo(Context context, List<File> src_movies) throws IOException {
        if(src_movies.isEmpty()) return false;

        int l = src_movies.size();
        Movie[] in_movies = new Movie[l];
        for(int i=0; i<l; i++) {
            in_movies[i] = MovieCreator.build(src_movies.get(i).toString());
        }

        List<Track> videoTracks = new LinkedList<>();
        List<Track> audioTracks = new LinkedList<>();

        for (Movie m : in_movies) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }
            }
        }

        Movie result = new Movie();

        if (audioTracks.size() > 0) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }

        String file_path = CameraHelper.getOutputMediaFilePath(CameraHelper.MEDIA_TYPE_VIDEO);
        if(file_path == null) return false;

        BasicContainer out = (BasicContainer) new DefaultMp4Builder().build(result);
        FileChannel fc = new RandomAccessFile(file_path,"rw").getChannel();
        out.writeContainer(fc);
        fc.close();
        File file = new File(file_path);
        CameraHelper.registerToMediaScanner(context, file);

        return true;
    }

}
