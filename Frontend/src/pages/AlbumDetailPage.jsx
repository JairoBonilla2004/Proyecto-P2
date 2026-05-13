import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { albumApi } from '../api/albumApi';
import ImageGrid from '../components/organisms/ImageGrid';
import Button from '../components/atoms/Button';
import Badge from '../components/atoms/Badge';
import UploadImageModal from '../components/organisms/UploadImageModal';
import { useAuth } from '../context/AuthContext';

const AlbumDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [album, setAlbum] = useState(null);
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showUploadModal, setShowUploadModal] = useState(false);

  const fetchAlbumAndImages = async () => {
    try {
      setLoading(true);
      let foundAlbum = null;
      try {
        const pubRes = await albumApi.getPublicAlbums();
        foundAlbum = pubRes.data.find(a => a.id === id);
      } catch (e) {}

      if (!foundAlbum && user) {
        try {
          const myRes = await albumApi.getMyAlbums();
          foundAlbum = myRes.data.find(a => a.id === id);
        } catch (e) {}
      }

      if (foundAlbum) {
        setAlbum(foundAlbum);
        const imgsRes = await albumApi.getAlbumImages(id);
        setImages(imgsRes.data);
      } else {
        navigate('/');
      }
    } catch (err) {
      console.error(err);
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlbumAndImages();
  }, [id, user]);

  if (loading) {
    return <div className="text-center py-20"><div className="w-12 h-12 rounded-full border-4 border-purple-500/30 border-t-purple-500 animate-spin mx-auto"></div></div>;
  }

  if (!album) return null;

  const isOwner = user && user.email === album.ownerEmail;

  return (
    <div className="fade-in">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <h1 className="text-3xl font-bold text-white">{album.title}</h1>
            <Badge status={album.approvalStatus} />
          </div>
          <p className="text-gray-400">{album.description}</p>
          <p className="text-sm text-gray-500 mt-1">Propietario: {album.ownerEmail}</p>
        </div>
        
        {isOwner && (
          <Button onClick={() => setShowUploadModal(true)}>
            <svg className="w-5 h-5 mr-2 -ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
            </svg>
            Subir Imagen
          </Button>
        )}
      </div>

      <ImageGrid images={images} loading={loading} />

      {showUploadModal && (
        <UploadImageModal 
          albumId={album.id} 
          onClose={() => setShowUploadModal(false)}
          onSuccess={fetchAlbumAndImages}
        />
      )}
    </div>
  );
};

export default AlbumDetailPage;
