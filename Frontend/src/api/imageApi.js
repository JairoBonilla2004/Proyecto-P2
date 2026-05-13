import axiosClient from './axiosClient';

export const imageApi = {
  uploadImage: async (albumId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post(`/albums/${albumId}/images`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};
