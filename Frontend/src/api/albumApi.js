import axiosClient from './axiosClient';

export const albumApi = {
  getPublicAlbums: async () => {
    const response = await axiosClient.get('/albums');
    return response.data;
  },
  getMyAlbums: async () => {
    const response = await axiosClient.get('/albums/mine');
    return response.data;
  },
  createAlbum: async (albumData) => {
    const response = await axiosClient.post('/albums', albumData);
    return response.data;
  },
  getAlbumImages: async (albumId) => {
    const response = await axiosClient.get(`/albums/${albumId}/images`);
    return response.data;
  }
};
