import React, { useRef, useEffect } from 'react';
import './Chart.css';

export type ChartType = 'line' | 'bar' | 'pie';
export type ChartDataPoint = {
  label: string;
  value: number;
  color?: string;
};

export interface ChartProps {
  type: ChartType;
  data: ChartDataPoint[];
  title?: string;
  width?: number | string;
  height?: number | string;
  showLegend?: boolean;
  showValues?: boolean;
  className?: string;
  animate?: boolean;
  colors?: string[];
  backgroundColor?: string;
  gridLines?: boolean;
}

// Default colors for the chart
const defaultColors = [
  '#94196B', // Primary color
  '#D63384',
  '#6F42C1',
  '#6610F2',
  '#0D6EFD',
  '#0DCAF0',
  '#20C997',
  '#198754',
  '#FFC107',
  '#FD7E14',
  '#DC3545'
];

const Chart: React.FC<ChartProps> = ({
  type,
  data,
  title,
  width = '100%',
  height = 300,
  showLegend = true,
  showValues = true,
  className = '',
  animate = true,
  colors = defaultColors,
  backgroundColor = 'var(--white)',
  gridLines = true
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  
  // Assign colors to data points if not provided
  const processedData = data.map((point, index) => ({
    ...point,
    color: point.color || colors[index % colors.length]
  }));
  
  // Draw the chart
  useEffect(() => {
    if (!canvasRef.current) return;
    
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Set background
    ctx.fillStyle = backgroundColor;
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw chart based on type
    switch (type) {
      case 'line':
        drawLineChart(ctx, canvas, processedData, {
          showValues,
          animate,
          gridLines
        });
        break;
      case 'bar':
        drawBarChart(ctx, canvas, processedData, {
          showValues,
          animate,
          gridLines
        });
        break;
      case 'pie':
        drawPieChart(ctx, canvas, processedData, {
          showValues,
          animate
        });
        break;
    }
  }, [type, processedData, showValues, animate, backgroundColor, gridLines]);
  
  // Container classes
  const chartClasses = [
    'chart-container',
    `chart-${type}`,
    className
  ].filter(Boolean).join(' ');
  
  return (
    <div 
      className={chartClasses} 
      style={{ 
        width: typeof width === 'number' ? `${width}px` : width,
        height: typeof height === 'number' ? `${height}px` : height 
      }}
    >
      {title && <h3 className="chart-title">{title}</h3>}
      <div className="chart-canvas-container">
        <canvas 
          ref={canvasRef}
          width={500}
          height={300}
        />
      </div>
      {showLegend && (
        <div className="chart-legend">
          {processedData.map((point, index) => (
            <div key={index} className="legend-item">
              <span className="legend-color" style={{ backgroundColor: point.color }}></span>
              <span className="legend-label">{point.label}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// Draw line chart
function drawLineChart(
  ctx: CanvasRenderingContext2D, 
  canvas: HTMLCanvasElement, 
  data: ChartDataPoint[],
  options: {
    showValues: boolean;
    animate: boolean;
    gridLines: boolean;
  }
) {
  const { width, height } = canvas;
  const padding = 40;
  const chartWidth = width - (padding * 2);
  const chartHeight = height - (padding * 2);
  
  // Find max value for scaling
  const maxValue = Math.max(...data.map(point => point.value));
  
  // Draw grid lines
  if (options.gridLines) {
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.1)';
    ctx.lineWidth = 1;
    
    // Horizontal grid lines
    const gridCount = 5;
    for (let i = 0; i <= gridCount; i++) {
      const y = padding + (chartHeight / gridCount) * i;
      ctx.beginPath();
      ctx.moveTo(padding, y);
      ctx.lineTo(width - padding, y);
      ctx.stroke();
      
      // Draw value labels
      if (options.showValues) {
        const value = Math.round((maxValue - (maxValue / gridCount) * i) * 100) / 100;
        ctx.fillStyle = 'var(--dark-gray)';
        ctx.font = '12px var(--font-family)';
        ctx.textAlign = 'right';
        ctx.fillText(value.toString(), padding - 10, y + 4);
      }
    }
  }
  
  // Draw x-axis labels
  ctx.fillStyle = 'var(--dark-gray)';
  ctx.font = '12px var(--font-family)';
  ctx.textAlign = 'center';
  
  data.forEach((point, index) => {
    const x = padding + (chartWidth / (data.length - 1 || 1)) * index;
    ctx.fillText(point.label, x, height - padding + 20);
  });
  
  // Draw line
  ctx.strokeStyle = data[0]?.color || defaultColors[0];
  ctx.lineWidth = 2;
  ctx.beginPath();
  
  data.forEach((point, index) => {
    const x = padding + (chartWidth / (data.length - 1 || 1)) * index;
    const y = padding + chartHeight - (chartHeight * (point.value / maxValue));
    
    if (index === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });
  
  ctx.stroke();
  
  // Draw points
  data.forEach((point, index) => {
    const x = padding + (chartWidth / (data.length - 1 || 1)) * index;
    const y = padding + chartHeight - (chartHeight * (point.value / maxValue));
    
    ctx.fillStyle = point.color || defaultColors[0];
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fill();
    
    // Draw value above point
    if (options.showValues) {
      ctx.fillStyle = 'var(--black)';
      ctx.font = 'bold 12px var(--font-family)';
      ctx.textAlign = 'center';
      ctx.fillText(point.value.toString(), x, y - 10);
    }
  });
}

// Draw bar chart
function drawBarChart(
  ctx: CanvasRenderingContext2D, 
  canvas: HTMLCanvasElement, 
  data: ChartDataPoint[],
  options: {
    showValues: boolean;
    animate: boolean;
    gridLines: boolean;
  }
) {
  const { width, height } = canvas;
  const padding = 40;
  const chartWidth = width - (padding * 2);
  const chartHeight = height - (padding * 2);
  
  // Find max value for scaling
  const maxValue = Math.max(...data.map(point => point.value));
  
  // Draw grid lines
  if (options.gridLines) {
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.1)';
    ctx.lineWidth = 1;
    
    // Horizontal grid lines
    const gridCount = 5;
    for (let i = 0; i <= gridCount; i++) {
      const y = padding + (chartHeight / gridCount) * i;
      ctx.beginPath();
      ctx.moveTo(padding, y);
      ctx.lineTo(width - padding, y);
      ctx.stroke();
      
      // Draw value labels
      if (options.showValues) {
        const value = Math.round((maxValue - (maxValue / gridCount) * i) * 100) / 100;
        ctx.fillStyle = 'var(--dark-gray)';
        ctx.font = '12px var(--font-family)';
        ctx.textAlign = 'right';
        ctx.fillText(value.toString(), padding - 10, y + 4);
      }
    }
  }
  
  // Calculate bar width
  const barWidth = chartWidth / data.length * 0.7;
  const barSpacing = chartWidth / data.length * 0.3;
  
  // Draw bars
  data.forEach((point, index) => {
    const x = padding + (chartWidth / data.length) * index + barSpacing / 2;
    const barHeight = chartHeight * (point.value / maxValue);
    const y = padding + chartHeight - barHeight;
    
    // Draw bar
    ctx.fillStyle = point.color || defaultColors[0];
    ctx.fillRect(x, y, barWidth, barHeight);
    
    // Draw label
    ctx.fillStyle = 'var(--dark-gray)';
    ctx.font = '12px var(--font-family)';
    ctx.textAlign = 'center';
    ctx.fillText(point.label, x + barWidth / 2, height - padding + 20);
    
    // Draw value above bar
    if (options.showValues) {
      ctx.fillStyle = 'var(--black)';
      ctx.font = 'bold 12px var(--font-family)';
      ctx.textAlign = 'center';
      ctx.fillText(point.value.toString(), x + barWidth / 2, y - 10);
    }
  });
}

// Draw pie chart
function drawPieChart(
  ctx: CanvasRenderingContext2D, 
  canvas: HTMLCanvasElement, 
  data: ChartDataPoint[],
  options: {
    showValues: boolean;
    animate: boolean;
  }
) {
  const { width, height } = canvas;
  const centerX = width / 2;
  const centerY = height / 2;
  const radius = Math.min(width, height) / 2.5;
  
  // Calculate total for percentages
  const total = data.reduce((sum, point) => sum + point.value, 0);
  
  // Draw pie slices
  let startAngle = 0;
  
  data.forEach((point) => {
    const sliceAngle = (point.value / total) * 2 * Math.PI;
    
    ctx.fillStyle = point.color || defaultColors[0];
    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle);
    ctx.closePath();
    ctx.fill();
    
    // Add stroke
    ctx.strokeStyle = 'var(--white)';
    ctx.lineWidth = 2;
    ctx.stroke();
    
    // Draw percentage or value label
    if (options.showValues) {
      const percentage = Math.round((point.value / total) * 100);
      const labelAngle = startAngle + sliceAngle / 2;
      const labelRadius = radius * 0.7;
      const labelX = centerX + Math.cos(labelAngle) * labelRadius;
      const labelY = centerY + Math.sin(labelAngle) * labelRadius;
      
      ctx.fillStyle = 'var(--white)';
      ctx.font = 'bold 12px var(--font-family)';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      
      if (percentage > 5) { // Only draw if slice is large enough
        ctx.fillText(`${percentage}%`, labelX, labelY);
      }
    }
    
    startAngle += sliceAngle;
  });
}

export default Chart; 