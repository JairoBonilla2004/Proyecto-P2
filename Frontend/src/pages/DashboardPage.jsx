import React, { useState, useEffect } from 'react';
import { albumApi } from '../api/albumApi';
import AlbumGrid from '../components/organisms/AlbumGrid';
import Button from '../components/atoms/Button';
import CreateAlbumModal from '../components/organisms/CreateAlbumModal';
import { useAuth } from '../context/AuthContext';

const DashboardPage = () => {
  const [albums, setAlbums] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const { user } = useAuth();

  const fetchMyAlbums = async () => {
    try {
      setLoading(true);
      const res = await albumApi.getMyAlbums();
      setAlbums(res.data);
    } catch (err) {
      console.error('Error fetching my albums', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyAlbums();
  }, []);

  return (
    <div className="fade-in">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-white">Mis Álbumes</h1>
          <p className="text-gray-400 mt-1">Gestiona tus colecciones fotográficas, {user?.email}.</p>
        </div>
        <Button onClick={() => setShowModal(true)}>
          <svg className="w-5 h-5 mr-2 -ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Crear Álbum
        </Button>
      </div>
      
      <AlbumGrid albums={albums} loading={loading} />

      {showModal && (
        <CreateAlbumModal 
          onClose={() => setShowModal(false)}
          onSuccess={() => {
            setShowModal(false);
            fetchMyAlbums();
          }}
        />
      )}
    </div>
  );
};

export default DashboardPage;
