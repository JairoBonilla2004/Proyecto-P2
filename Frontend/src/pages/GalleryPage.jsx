import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { albumApi } from '../api/albumApi';
import AlbumGrid from '../components/organisms/AlbumGrid';
import { useAuth } from '../context/AuthContext';

const GalleryPage = () => {
  const [albums, setAlbums] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  useEffect(() => {
    const fetchAlbums = async () => {
      try {
        const res = await albumApi.getPublicAlbums();
        setAlbums(res.data);
      } catch (err) {
        console.error('Error fetching public albums', err);
      } finally {
        setLoading(false);
      }
    };
    fetchAlbums();
  }, []);

  return (
    <div className="fade-in">
      <div className="bg-mesh"></div>
      
      {/* Premium Hero Section */}
      <section className="relative py-24 lg:py-36 flex flex-col items-center justify-center overflow-hidden mb-20 rounded-[40px] glass-card border-white/5 bg-white/[0.02]">
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-[#050505]/50 to-[#030305] z-0"></div>
        <div className="relative z-10 text-center px-4 max-w-4xl mx-auto">
          <div className="inline-flex items-center gap-3 px-4 py-1.5 rounded-full bg-white/5 border border-white/10 mb-8 backdrop-blur-xl shadow-[0_0_20px_rgba(0,240,255,0.1)]">
            <span className="w-2.5 h-2.5 rounded-full bg-[#00f0ff] animate-pulse"></span>
            <span className="text-sm font-semibold tracking-wide text-gray-200">Seguridad SDLC Activa</span>
          </div>
          
          <h1 className="text-5xl md:text-7xl lg:text-8xl font-extrabold tracking-tight mb-8 leading-[1.1]">
            Next-Gen <br className="md:hidden" />
            <span className="text-gradient">Secure Media</span> Hub
          </h1>
          
          <p className="text-lg md:text-2xl text-gray-400 mb-12 max-w-3xl mx-auto leading-relaxed font-medium">
            Plataforma de grado empresarial para compartir imágenes. Protegida por análisis esteganográfico avanzado y algoritmos de entropía LSB.
          </p>
          
          <div className="flex flex-col sm:flex-row items-center justify-center gap-5">
            <a href="#explore" className="btn-premium text-lg px-8 py-4 w-full sm:w-auto">
              Explorar Colecciones
            </a>
            {!user && (
              <Link to="/register" className="text-lg px-8 py-4 w-full sm:w-auto rounded-full font-bold bg-white/5 text-white hover:bg-white/10 transition-all border border-white/10 backdrop-blur-md">
                Crear tu cuenta
              </Link>
            )}
          </div>
        </div>

        {/* Decorative floating elements */}
        <div className="hidden lg:block absolute left-20 top-20 w-48 h-48 bg-[#00f0ff] rounded-full mix-blend-screen filter blur-[80px] opacity-20 animate-pulse"></div>
        <div className="hidden lg:block absolute right-20 bottom-20 w-64 h-64 bg-[#ff0055] rounded-full mix-blend-screen filter blur-[100px] opacity-20 animate-pulse delay-1000"></div>
      </section>

      {/* Main Content */}
      <section id="explore" className="relative z-10 scroll-mt-32">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-10 gap-4">
          <div>
            <h2 className="text-4xl font-bold font-display text-white mb-3">Colecciones Verificadas</h2>
            <p className="text-lg text-gray-400">Todo el contenido ha pasado estrictos controles contra inyección de payloads.</p>
          </div>
        </div>
        
        <AlbumGrid albums={albums} loading={loading} />
      </section>
    </div>
  );
};

export default GalleryPage;
