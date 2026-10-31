import { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Tooltip, useMap } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { HospitalListing } from '../types'

function MapViewport({ hospitals, selectedId }: { hospitals: HospitalListing[]; selectedId?: number }) {
  const map = useMap()
  useEffect(() => {
    const positions = hospitals.flatMap(h => h.position ? [h.position] : [])
    if (positions.length > 1) map.fitBounds(L.latLngBounds(positions), { padding: [65, 65], maxZoom: 12 })
    else if (positions.length === 1) map.setView(positions[0], 12)
  }, [hospitals, map])
  useEffect(() => {
    const selected = hospitals.find(h => h.id === selectedId)
    if (selected?.position) map.panTo(selected.position, { animate: true })
  }, [selectedId, hospitals, map])
  return null
}

export default function HospitalMap({ hospitals, selectedId, onSelect }: {
  hospitals: HospitalListing[]; selectedId?: number; onSelect: (hospital: HospitalListing) => void;
}) {
  return <MapContainer center={[34.010, -6.840]} zoom={12} scrollWheelZoom={false} className="hospital-map" attributionControl>
    <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" />
    <MapViewport hospitals={hospitals} selectedId={selectedId} />
    {hospitals.filter(h => h.position).map((hospital, index) => <Marker key={hospital.id} position={hospital.position!}
      title={hospital.name} eventHandlers={{ click: () => onSelect(hospital) }}
      icon={L.divIcon({ className: `hospital-marker ${hospital.status.toLowerCase()} ${hospital.id === selectedId ? 'is-selected' : ''}`,
        html: `<span><b>${index + 1}</b></span>`, iconSize: [35, 42], iconAnchor: [17, 42] })}>
      <Tooltip direction="top" offset={[0, -35]}>{hospital.name}</Tooltip>
    </Marker>)}
  </MapContainer>
}
