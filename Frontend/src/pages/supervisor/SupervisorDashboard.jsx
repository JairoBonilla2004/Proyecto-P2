import React, { useState, useEffect } from 'react';
import { supervisorApi } from '../../api/supervisorApi';
import QuarantineQueue from '../../components/organisms/QuarantineQueue';
import SupervisorAlbumList from '../../components/organisms/SupervisorAlbumList';

const SupervisorDashboard = () => {
  const [activeTab, setActiveTab] = useState('quarantine'); // quarantine | albums
  const [quarantineQueue, setQuarantineQueue] = useState([]);
  const [pendingAlbums, setPendingAlbums] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'quarantine') {
        const res = await supervisorApi.getQuarantineQueue();
        setQuarantineQueue(res.data);
      } else {
        const res = await supervisorApi.getPendingAlbums();
        setPendingAlbums(res.data);
      }
    } catch (error) {
      console.error('Error fetching supervisor data', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [activeTab]);

  const handleApproveImage = async (id) => {
    setProcessingId(id);
    try {
      await supervisorApi.approveImage(id, 'Revisión manual: Falso positivo LSB');
      fetchData();
    } catch (error) {
      console.error('Error', error);
    } finally {
      setProcessingId(null);
    }
  };

  const handleRejectImage = async (id) => {
    setProcessingId(id);
    try {
      await supervisorApi.rejectImage(id, 'Revisión manual: Contenido oculto confirmado');
      fetchData();
    } catch (error) {
      console.error('Error', error);
    } finally {
      setProcessingId(null);
    }
  };

  const handleApproveAlbum = async (id) => {
    setProcessingId(id);
    try {
      await supervisorApi.approveAlbum(id);
      fetchData();
    } catch (error) {
      console.error('Error', error);
    } finally {
      setProcessingId(null);
    }
  };

  const handleRejectAlbum = async (id) => {
    setProcessingId(id);
    try {
      await supervisorApi.rejectAlbum(id);
      fetchData();
    } catch (error) {
      console.error('Error', error);
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="fade-in">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white flex items-center gap-3">
          <svg className="w-8 h-8 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          Panel de Supervisor
        </h1>
        <p className="text-gray-400 mt-2">Revisión de esteganografía y aprobación de álbumes públicos.</p>
      </div>

      <div className="flex gap-4 border-b border-white/10 mb-6">
        <button
          onClick={() => setActiveTab('quarantine')}
          className={`pb-3 px-2 text-sm font-medium transition-colors border-b-2 ${activeTab === 'quarantine' ? 'border-purple-500 text-purple-400' : 'border-transparent text-gray-400 hover:text-gray-200'}`}
        >
          Cola de Cuarentena ({activeTab === 'quarantine' && !loading ? quarantineQueue.length : '-'})
        </button>
        <button
          onClick={() => setActiveTab('albums')}
          className={`pb-3 px-2 text-sm font-medium transition-colors border-b-2 ${activeTab === 'albums' ? 'border-purple-500 text-purple-400' : 'border-transparent text-gray-400 hover:text-gray-200'}`}
        >
          Álbumes Pendientes ({activeTab === 'albums' && !loading ? pendingAlbums.length : '-'})
        </button>
      </div>

      <div>
        {activeTab === 'quarantine' ? (
          <QuarantineQueue 
            queue={quarantineQueue} 
            loading={loading} 
            onApprove={handleApproveImage}
            onReject={handleRejectImage}
            processingId={processingId}
          />
        ) : (
          <SupervisorAlbumList 
            albums={pendingAlbums}
            loading={loading}
            onApprove={handleApproveAlbum}
            onReject={handleRejectAlbum}
            processingId={processingId}
          />
        )}
      </div>
    </div>
  );
};

export default SupervisorDashboard;
