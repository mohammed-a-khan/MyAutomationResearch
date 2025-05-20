import React, { useRef, useEffect, useState } from 'react';
import './Chart.css';

export interface DataPoint {
  x: number | string;
  y: number;
}

export interface LineChartSeries {
  id: string;
  name: string;
  data: DataPoint[];
  color?: string;
  strokeWidth?: number;
  areaFill?: boolean;
  areaOpacity?: number;
}

export interface LineChartProps {
  series: LineChartSeries[];
  width?: number | string;
  height?: number | string;
  xAxisTitle?: string;
  yAxisTitle?: string;
  xAxisType?: 'category' | 'datetime' | 'numeric';
  showGrid?: boolean;
  showLegend?: boolean;
  showTooltip?: boolean;
  showPoints?: boolean;
  animate?: boolean;
  responsive?: boolean;
  className?: string;
  customColors?: string[];
  minY?: number;
  maxY?: number;
  formatXLabel?: (value: string | number) => string;
  formatYLabel?: (value: number) => string;
  formatTooltip?: (series: string, x: string | number, y: number) => string;
}

const LineChart: React.FC<LineChartProps> = ({
  series,
  width = '100%',
  height = '300px',
  xAxisTitle,
  yAxisTitle,
  xAxisType = 'category',
  showGrid = true,
  showLegend = true,
  showTooltip = true,
  showPoints = true,
  animate = true,
  responsive = true,
  className = '',
  customColors,
  minY: providedMinY,
  maxY: providedMaxY,
  formatXLabel,
  formatYLabel,
  formatTooltip
}) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [tooltip, setTooltip] = useState<{ visible: boolean; x: number; y: number; content: string }>({
    visible: false,
    x: 0,
    y: 0,
    content: ''
  });

  // Default colors if custom colors are not provided
  const defaultColors = [
    '#94196B', // Primary color
    '#3B82F6',
    '#10B981',
    '#F59E0B',
    '#EF4444',
    '#8B5CF6',
    '#EC4899',
    '#06B6D4',
    '#14B8A6',
    '#F97316'
  ];

  // Use custom colors or default colors
  const colors = customColors || defaultColors;

  // Calculate initial dimensions and update on resize if responsive
  useEffect(() => {
    const updateDimensions = () => {
      if (containerRef.current && responsive) {
        const { width } = containerRef.current.getBoundingClientRect();
        // Use a ratio to calculate the height if it's a percentage
        const heightValue = typeof height === 'string' && height.includes('%') 
          ? width * (parseInt(height) / 100)
          : typeof height === 'string' 
            ? parseInt(height) 
            : height;
            
        setDimensions({
          width,
          height: typeof heightValue === 'number' ? heightValue : 300
        });
      }
    };

    updateDimensions();

    const debouncedUpdateDimensions = debounce(updateDimensions, 250);
    window.addEventListener('resize', debouncedUpdateDimensions);
    
    return () => {
      window.removeEventListener('resize', debouncedUpdateDimensions);
    };
  }, [responsive, height]);

  // Debounce function to limit resize events
  function debounce(func: (...args: any[]) => void, wait: number) {
    let timeout: ReturnType<typeof setTimeout> | null = null;
    
    return function(...args: any[]) {
      const later = () => {
        timeout = null;
        func(...args);
      };
      
      if (timeout) clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }

  // Create the chart when dimensions change or data changes
  useEffect(() => {
    if (!svgRef.current || !containerRef.current || dimensions.width === 0) return;

    // Clear previous contents
    const svg = svgRef.current;
    while (svg.firstChild) {
      svg.removeChild(svg.firstChild);
    }

    // If no series data, don't render
    if (series.length === 0 || series.some(s => s.data.length === 0)) {
      return;
    }

    // Chart dimensions
    const margin = { top: 30, right: 30, bottom: 50, left: 60 };
    const width = dimensions.width;
    const height = dimensions.height;
    const innerWidth = width - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;

    // Create chart container group
    const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    g.setAttribute('transform', `translate(${margin.left},${margin.top})`);
    svg.appendChild(g);

    // Prepare all X values for domain calculation
    let allXValues: (string | number)[] = [];
    series.forEach(s => {
      allXValues = allXValues.concat(s.data.map(d => d.x));
    });

    // Remove duplicates from X values if they are strings
    if (xAxisType === 'category') {
      allXValues = Array.from(new Set(allXValues));
    }

    // Sort X values if they are numeric or dates
    if (xAxisType === 'numeric') {
      allXValues.sort((a, b) => Number(a) - Number(b));
    } else if (xAxisType === 'datetime') {
      allXValues.sort((a, b) => new Date(a.toString()).getTime() - new Date(b.toString()).getTime());
    }

    // Find min and max Y values
    let minY = Number.MAX_VALUE;
    let maxY = Number.MIN_VALUE;

    series.forEach(s => {
      s.data.forEach(d => {
        minY = Math.min(minY, d.y);
        maxY = Math.max(maxY, d.y);
      });
    });

    // Apply provided min/max if specified
    if (providedMinY !== undefined) minY = providedMinY;
    if (providedMaxY !== undefined) maxY = providedMaxY;

    // Add some padding to the Y domain
    const yPadding = (maxY - minY) * 0.1;
    minY = Math.floor(minY - yPadding);
    maxY = Math.ceil(maxY + yPadding);

    // Create X scale
    let xScale: (value: string | number) => number;
    
    if (xAxisType === 'numeric') {
      const domain = [Number(allXValues[0]), Number(allXValues[allXValues.length - 1])];
      xScale = (value: string | number) => {
        return (Number(value) - domain[0]) / (domain[1] - domain[0]) * innerWidth;
      };
    } else if (xAxisType === 'datetime') {
      const domain = [
        new Date(allXValues[0].toString()).getTime(),
        new Date(allXValues[allXValues.length - 1].toString()).getTime()
      ];
      xScale = (value: string | number) => {
        const time = new Date(value.toString()).getTime();
        return (time - domain[0]) / (domain[1] - domain[0]) * innerWidth;
      };
    } else {
      // Category scale
      xScale = (value: string | number) => {
        const index = allXValues.indexOf(value);
        return index * (innerWidth / (allXValues.length - 1));
      };
    }

    // Create Y scale
    const yScale = (value: number) => {
      return innerHeight - ((value - minY) / (maxY - minY) * innerHeight);
    };

    // Draw grid if enabled
    if (showGrid) {
      // Horizontal grid lines
      const yTickCount = 5;
      const yStep = (maxY - minY) / (yTickCount - 1);
      
      for (let i = 0; i < yTickCount; i++) {
        const yValue = minY + i * yStep;
        const y = yScale(yValue);
        
        const gridLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        gridLine.setAttribute('x1', '0');
        gridLine.setAttribute('y1', String(y));
        gridLine.setAttribute('x2', String(innerWidth));
        gridLine.setAttribute('y2', String(y));
        gridLine.setAttribute('class', 'chart-grid-line');
        g.appendChild(gridLine);
      }

      // Vertical grid lines
      const tickCount = xAxisType === 'category' ? allXValues.length : 5;
      for (let i = 0; i < tickCount; i++) {
        const x = xAxisType === 'category' 
          ? i * (innerWidth / (allXValues.length - 1))
          : i * (innerWidth / (tickCount - 1));
        
        const gridLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        gridLine.setAttribute('x1', String(x));
        gridLine.setAttribute('y1', '0');
        gridLine.setAttribute('x2', String(x));
        gridLine.setAttribute('y2', String(innerHeight));
        gridLine.setAttribute('class', 'chart-grid-line');
        g.appendChild(gridLine);
      }
    }

    // Draw X and Y axes
    // X axis
    const xAxis = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    xAxis.setAttribute('x1', '0');
    xAxis.setAttribute('y1', String(innerHeight));
    xAxis.setAttribute('x2', String(innerWidth));
    xAxis.setAttribute('y2', String(innerHeight));
    xAxis.setAttribute('class', 'chart-axis');
    g.appendChild(xAxis);

    // X axis title
    if (xAxisTitle) {
      const xTitle = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      xTitle.setAttribute('x', String(innerWidth / 2));
      xTitle.setAttribute('y', String(innerHeight + 40));
      xTitle.setAttribute('text-anchor', 'middle');
      xTitle.setAttribute('class', 'chart-axis-title');
      xTitle.textContent = xAxisTitle;
      g.appendChild(xTitle);
    }

    // X axis ticks
    const xTickValues = xAxisType === 'category' 
      ? allXValues 
      : Array.from({ length: 5 }, (_, i) => {
          if (xAxisType === 'numeric') {
            const min = Number(allXValues[0]);
            const max = Number(allXValues[allXValues.length - 1]);
            return min + i * (max - min) / 4;
          } else {
            const min = new Date(allXValues[0].toString()).getTime();
            const max = new Date(allXValues[allXValues.length - 1].toString()).getTime();
            return new Date(min + i * (max - min) / 4).toISOString();
          }
        });

    xTickValues.forEach((tickValue, index) => {
      const x = xScale(tickValue);
      
      // Tick mark
      const tick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      tick.setAttribute('x1', String(x));
      tick.setAttribute('y1', String(innerHeight));
      tick.setAttribute('x2', String(x));
      tick.setAttribute('y2', String(innerHeight + 6));
      tick.setAttribute('class', 'chart-tick');
      g.appendChild(tick);

      // Tick label
      const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      label.setAttribute('x', String(x));
      label.setAttribute('y', String(innerHeight + 20));
      label.setAttribute('text-anchor', 'middle');
      label.setAttribute('class', 'chart-tick-label');
      
      let formattedLabel = formatXLabel ? formatXLabel(tickValue) : String(tickValue);
      
      // If datetime, format as date
      if (xAxisType === 'datetime' && !formatXLabel) {
        formattedLabel = new Date(tickValue.toString()).toLocaleDateString();
      }
      
      // Truncate long labels
      if (formattedLabel.length > 10) {
        formattedLabel = formattedLabel.substring(0, 10) + '...';
      }
      
      label.textContent = formattedLabel;
      g.appendChild(label);
    });

    // Y axis
    const yAxis = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    yAxis.setAttribute('x1', '0');
    yAxis.setAttribute('y1', '0');
    yAxis.setAttribute('x2', '0');
    yAxis.setAttribute('y2', String(innerHeight));
    yAxis.setAttribute('class', 'chart-axis');
    g.appendChild(yAxis);

    // Y axis title
    if (yAxisTitle) {
      const yTitle = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      yTitle.setAttribute('transform', `rotate(-90) translate(${-innerHeight / 2}, ${-40})`);
      yTitle.setAttribute('text-anchor', 'middle');
      yTitle.setAttribute('class', 'chart-axis-title');
      yTitle.textContent = yAxisTitle;
      g.appendChild(yTitle);
    }

    // Y axis ticks
    const yTickCount = 5;
    const yStep = (maxY - minY) / (yTickCount - 1);
    
    for (let i = 0; i < yTickCount; i++) {
      const yValue = minY + i * yStep;
      const y = yScale(yValue);
      
      // Tick mark
      const tick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      tick.setAttribute('x1', '-6');
      tick.setAttribute('y1', String(y));
      tick.setAttribute('x2', '0');
      tick.setAttribute('y2', String(y));
      tick.setAttribute('class', 'chart-tick');
      g.appendChild(tick);

      // Tick label
      const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      label.setAttribute('x', '-10');
      label.setAttribute('y', String(y));
      label.setAttribute('text-anchor', 'end');
      label.setAttribute('dominant-baseline', 'middle');
      label.setAttribute('class', 'chart-tick-label');
      
      const formattedLabel = formatYLabel ? formatYLabel(yValue) : yValue.toLocaleString();
      label.textContent = formattedLabel;
      g.appendChild(label);
    }

    // Draw data series
    series.forEach((s, seriesIndex) => {
      const color = s.color || colors[seriesIndex % colors.length];
      const strokeWidth = s.strokeWidth || 2;
      
      // Create path for line
      const linePath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      let d = '';
      
      s.data.forEach((point, i) => {
        const x = xScale(point.x);
        const y = yScale(point.y);
        if (i === 0) {
          d += `M ${x} ${y}`;
        } else {
          d += ` L ${x} ${y}`;
        }
      });
      
      linePath.setAttribute('d', d);
      linePath.setAttribute('class', 'chart-line');
      linePath.setAttribute('stroke', color);
      linePath.setAttribute('stroke-width', String(strokeWidth));
      linePath.setAttribute('fill', 'none');
      
      // Animation for line
      if (animate) {
        const length = linePath.getTotalLength();
        linePath.style.strokeDasharray = `${length}`;
        linePath.style.strokeDashoffset = `${length}`;
        linePath.style.animation = `chartLineAnimation 1.5s ease-in-out forwards`;
      }
      
      g.appendChild(linePath);

      // Create area fill if enabled
      if (s.areaFill) {
        const areaPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        let areaD = '';
        
        // Start at bottom left
        areaD += `M ${xScale(s.data[0].x)} ${innerHeight}`;
        
        // Draw line to first data point
        areaD += ` L ${xScale(s.data[0].x)} ${yScale(s.data[0].y)}`;
        
        // Draw lines to all other data points
        for (let i = 1; i < s.data.length; i++) {
          areaD += ` L ${xScale(s.data[i].x)} ${yScale(s.data[i].y)}`;
        }
        
        // Draw line to bottom right
        areaD += ` L ${xScale(s.data[s.data.length - 1].x)} ${innerHeight}`;
        
        // Close path
        areaD += ' Z';
        
        areaPath.setAttribute('d', areaD);
        areaPath.setAttribute('class', 'chart-area');
        areaPath.setAttribute('fill', color);
        areaPath.setAttribute('opacity', String(s.areaOpacity || 0.1));
        
        // Animation for area
        if (animate) {
          areaPath.style.animation = `chartAreaAnimation 1.5s ease-in-out forwards`;
        }
        
        g.appendChild(areaPath);
      }

      // Draw points if enabled
      if (showPoints) {
        s.data.forEach(point => {
          const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
          circle.setAttribute('cx', String(xScale(point.x)));
          circle.setAttribute('cy', String(yScale(point.y)));
          circle.setAttribute('r', '4');
          circle.setAttribute('class', 'chart-point');
          circle.setAttribute('fill', color);
          circle.setAttribute('stroke', 'white');
          circle.setAttribute('stroke-width', '2');
          
          // Show tooltip on hover
          if (showTooltip) {
            circle.addEventListener('mouseenter', (e) => {
              if (!tooltipRef.current) return;
              
              const tooltipContent = formatTooltip
                ? formatTooltip(s.name, point.x, point.y)
                : `${s.name}: ${typeof point.x === 'string' && xAxisType === 'datetime' 
                    ? new Date(point.x).toLocaleDateString() 
                    : point.x}, ${point.y}`;

              const rect = containerRef.current!.getBoundingClientRect();
              const offsetX = e.clientX - rect.left;
              const offsetY = e.clientY - rect.top;
              
              setTooltip({
                visible: true,
                x: offsetX,
                y: offsetY - 30, // Position above the point
                content: tooltipContent
              });
            });

            circle.addEventListener('mouseleave', () => {
              setTooltip(prev => ({ ...prev, visible: false }));
            });
          }
          
          // Animation for points
          if (animate) {
            circle.style.animation = `chartPointAnimation 0.3s ease-out ${s.data.indexOf(point) * 0.05 + 0.5}s forwards`;
            circle.style.opacity = '0';
          }
          
          g.appendChild(circle);
        });
      }
    });

    // Create legend if enabled
    if (showLegend && series.length > 0) {
      const legendG = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      legendG.setAttribute('class', 'chart-legend');
      
      const legendItemHeight = 20;
      const legendItemWidth = 150;
      const legendPadding = 5;
      const legendItems = Math.min(series.length, 5); // Show max 5 items in one row
      
      series.forEach((s, i) => {
        const color = s.color || colors[i % colors.length];
        const row = Math.floor(i / legendItems);
        const col = i % legendItems;
        
        // Legend item group
        const itemG = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        itemG.setAttribute('transform', `translate(${col * legendItemWidth}, ${row * legendItemHeight})`);
        
        // Color box
        const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        rect.setAttribute('width', '12');
        rect.setAttribute('height', '12');
        rect.setAttribute('y', '0');
        rect.setAttribute('rx', '2');
        rect.setAttribute('fill', color);
        itemG.appendChild(rect);
        
        // Series name
        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        text.setAttribute('x', '18');
        text.setAttribute('y', '10');
        text.setAttribute('dominant-baseline', 'middle');
        text.setAttribute('class', 'chart-legend-text');
        
        // Truncate long names
        let seriesName = s.name;
        if (seriesName.length > 15) {
          seriesName = seriesName.substring(0, 15) + '...';
        }
        
        text.textContent = seriesName;
        itemG.appendChild(text);
        
        legendG.appendChild(itemG);
      });
      
      // Calculate legend dimensions
      const legendRows = Math.ceil(series.length / legendItems);
      const legendHeight = legendRows * legendItemHeight + legendPadding * 2;
      const legendWidth = Math.min(series.length, legendItems) * legendItemWidth + legendPadding * 2;
      
      // Background rect for legend
      const bgRect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
      bgRect.setAttribute('width', String(legendWidth));
      bgRect.setAttribute('height', String(legendHeight));
      bgRect.setAttribute('rx', '4');
      bgRect.setAttribute('class', 'chart-legend-bg');
      legendG.insertBefore(bgRect, legendG.firstChild);
      
      // Position legend at the top right
      legendG.setAttribute('transform', `translate(${width - legendWidth - margin.right}, ${margin.top / 2})`);
      
      svg.appendChild(legendG);
    }
  }, [dimensions, series, animate, showGrid, showLegend, showPoints, showTooltip, xAxisTitle, yAxisTitle, xAxisType, formatXLabel, formatYLabel, formatTooltip, colors, providedMinY, providedMaxY]);

  return (
    <div 
      ref={containerRef} 
      className={`chart-container ${className}`}
      style={{ 
        width, 
        height,
        position: 'relative' 
      }}
    >
      <svg 
        ref={svgRef}
        width={dimensions.width || '100%'}
        height={dimensions.height}
        className="chart-svg"
      />
      {showTooltip && (
        <div
          ref={tooltipRef}
          className={`chart-tooltip ${tooltip.visible ? 'chart-tooltip-visible' : ''}`}
          style={{
            left: tooltip.x,
            top: tooltip.y
          }}
        >
          {tooltip.content}
        </div>
      )}
    </div>
  );
};

export default LineChart; 