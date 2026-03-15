import { useState } from 'react'
import axios from 'axios'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer
} from 'recharts'
import { format, parseISO } from 'date-fns'
import './index.css'

const API = import.meta.env.VITE_API_URL || ''

export default function App() {
  const [start,   setStart]   = useState('2024-01-05T00:00')
  const [end,     setEnd]     = useState('2024-01-05T23:30')
  const [horizon, setHorizon] = useState(4)
  const [data,    setData]    = useState(null)
  const [stats,   setStats]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  async function loadData() {
    setLoading(true)
    setError(null)
    try {
      const res = await axios.get(`${API}/api/chart`, {
        params: {
          start:   start + ':00Z',
          end:     end   + ':00Z',
          horizon
        }
      })
      setData(res.data.data)
      setStats(res.data.stats)
    } catch (e) {
      setError(e?.response?.data?.detail || e.message || 'Something went wrong')
      setData(null)
      setStats(null)
    } finally {
      setLoading(false)
    }
  }

  function formatTick(iso) {
    try { return format(parseISO(iso), 'dd MMM HH:mm') }
    catch { return iso }
  }

  // Custom tooltip
  function CustomTooltip({ active, payload, label }) {
    if (!active || !payload?.length) return null
    const actual   = payload.find(p => p.dataKey === 'actual')
    const forecast = payload.find(p => p.dataKey === 'forecast')
    const error    = actual?.value != null && forecast?.value != null
      ? (forecast.value - actual.value).toFixed(0) : null

    return (
      <div style={{ background: '#fff', border: '1px solid #ddd', padding: '10px', borderRadius: 4, fontSize: 12 }}>
        <p style={{ marginBottom: 6, color: '#666' }}>{formatTick(label)}</p>
        {actual?.value   != null && <p style={{ color: '#1a73e8' }}>Actual: {actual.value.toLocaleString()} MW</p>}
        {forecast?.value != null && <p style={{ color: '#34a853' }}>Forecast: {forecast.value.toLocaleString()} MW</p>}
        {error != null && <p style={{ color: Number(error) > 0 ? '#e53935' : '#34a853', marginTop: 4 }}>
          Error: {Number(error) > 0 ? '+' : ''}{error} MW
        </p>}
      </div>
    )
  }

  const ticks = data
    ? data.filter((_, i) => i % Math.max(1, Math.floor(data.length / 8)) === 0)
        .map(d => d.startTime)
    : []

  return (
    <>
      <div className="header">
        <h1>🌬️ Wind Forecast Monitor</h1>
        <span></span>
      </div>

      <div className="container">

        {/* Controls */}
        <div className="controls">
          <div className="field">
            <label>Start Time</label>
            <input type="datetime-local" value={start}
              min="2024-01-01T00:00" max="2024-01-31T23:30"
              onChange={e => setStart(e.target.value)} />
          </div>

          <div className="field">
            <label>End Time</label>
            <input type="datetime-local" value={end}
              min="2024-01-01T00:00" max="2024-01-31T23:30"
              onChange={e => setEnd(e.target.value)} />
          </div>

          <div className="field">
            <label>Forecast Horizon: {horizon}h</label>
            <input type="range" min={0} max={48} step={1}
              value={horizon} onChange={e => setHorizon(Number(e.target.value))} />
          </div>

          <button onClick={loadData} disabled={loading}>
            {loading ? 'Loading...' : 'Load Data'}
          </button>
        </div>

        {error && <div className="error">⚠ {error}</div>}

        <div className="chart-box">
          <div className="chart-title">Generation (MW)</div>

          <div className="legend">
            <div className="legend-item">
              <div className="dot" style={{ background: '#1a73e8' }} />
              <span>Actual</span>
            </div>
            <div className="legend-item">
              <div className="dot" style={{ background: '#34a853', backgroundImage: 'repeating-linear-gradient(90deg,#34a853 0,#34a853 6px,transparent 6px,transparent 10px)' }} />
              <span>Forecast</span>
            </div>
          </div>

          {!data && !loading && (
            <div className="empty">Select a time range and click Load Data</div>
          )}

          {loading && (
            <div className="empty">Loading data...</div>
          )}

          {data && (
            <ResponsiveContainer width="100%" height={380}>
              <LineChart data={data} margin={{ top: 4, right: 16, bottom: 4, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                <XAxis
                  dataKey="startTime"
                  ticks={ticks}
                  tickFormatter={formatTick}
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11 }}
                  tickFormatter={v => `${(v/1000).toFixed(1)}k`}
                  tickLine={false}
                  axisLine={false}
                  width={40}
                />
                <Tooltip content={<CustomTooltip />} />
                <Line
                  type="monotone"
                  dataKey="actual"
                  stroke="#1a73e8"
                  strokeWidth={2}
                  dot={false}
                  connectNulls={false}
                  name="Actual"
                />
                <Line
                  type="monotone"
                  dataKey="forecast"
                  stroke="#34a853"
                  strokeWidth={2}
                  strokeDasharray="6 3"
                  dot={false}
                  connectNulls={true}
                  name="Forecast"
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        {stats && stats.n > 0 && (
          <div className="stats">
            {[
              { label: 'Data Points', value: stats.n, unit: '' },
              { label: 'MAE',         value: stats.mae,  unit: ' MW' },
              { label: 'RMSE',        value: stats.rmse, unit: ' MW' },
              { label: 'Bias',        value: stats.bias, unit: ' MW' },
              { label: 'P50 Error',   value: stats.p50,  unit: ' MW' },
              { label: 'P99 Error',   value: stats.p99,  unit: ' MW' },
            ].map(({ label, value, unit }) => (
              <div className="stat-item" key={label}>
                <div className="stat-label">{label}</div>
                <div className="stat-value">{value}{unit}</div>
              </div>
            ))}
          </div>
        )}

      </div>
    </>
  )
}