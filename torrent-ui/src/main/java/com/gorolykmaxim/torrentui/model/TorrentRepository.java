package com.gorolykmaxim.torrentui.model;

public class TorrentRepository {

    private TorrentService service;

    public TorrentRepository(TorrentService service) {
        this.service = service;
    }

    public void save(Torrent torrent) {
        torrent.downloadVia(service);
    }

    public void deleteById(String id, boolean deleteData) {
        service.deleteTorrentById(id, deleteData);
    }

}
