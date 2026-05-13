import axiosClient from './axiosClient';

export const supervisorApi = {
  getPendingAlbums: async () => {
    const response = await axiosClient.get('/supervisor/albums/pending');
    return response.data;
  },
  approveAlbum: async (albumId) => {
    const response = await axiosClient.put(`/supervisor/albums/${albumId}/approve`);
    return response.data;
  },
  rejectAlbum: async (albumId) => {
    const response = await axiosClient.put(`/supervisor/albums/${albumId}/reject`);
    return response.data;
  },
  getQuarantineQueue: async () => {
    const response = await axiosClient.get('/supervisor/quarantine');
    return response.data;
  },
  getQuarantineHistory: async () => {
    const response = await axiosClient.get('/supervisor/quarantine/history');
    return response.data;
  },
  approveImage: async (imageId, notes) => {
    const response = await axiosClient.put(`/supervisor/images/${imageId}/approve`, { notes });
    return response.data;
  },
  rejectImage: async (imageId, notes) => {
    const response = await axiosClient.put(`/supervisor/images/${imageId}/reject`, { notes });
    return response.data;
  },
};
