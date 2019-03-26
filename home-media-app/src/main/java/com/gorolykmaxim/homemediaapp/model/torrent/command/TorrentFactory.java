package com.gorolykmaxim.homemediaapp.model.torrent.command;

public class TorrentFactory {

    public Torrent createMagnet(String magnetLink, String downloadFolder) {
        return new Torrent(new MagnetLinkStrategy(magnetLink, downloadFolder));
    }

}
