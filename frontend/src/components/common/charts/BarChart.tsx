import React, { useRef, useEffect, useState } from 'react';
import './Chart.css';

export interface BarChartData {
  id: string;
  label: string;
  value: number;
  color?: string;
}

export interface BarChartProps {
  data: BarChartData[];
  width?: number | string;
  height?: number | string;
  xAxisTitle?: string;
  yAxisTitle?: string;
  showGrid?: boolean;
  showLegend?: boolean;
  showTooltip?: boolean;
  animate?: boolean;
  responsive?: boolean;
  horizontal?: boolean;
  stacked?: boolean;
  className?: string;
  customColors?: string[];
  minY?: number;
  maxY?: number;
  formatXLabel?: (value: string) => string;
  formatYLabel?: (value: number) => string;
  formatTooltip?: (label: string, value: number) => string;
  barPadding?: number;
  barBorderRadius?: number;
  onBarClick?: (data: BarChartData) => void;
}

const BarChart: React.FC<BarChartProps> = ({
  data,
  width = '100%',
  height = '300px',
  xAxisTitle,
  yAxisTitle,
  showGrid = true,
  showLegend = true,
  showTooltip = true,
  animate = true,
  responsive = true,
  horizontal = false,
  stacked = false,
  className = '',
  customColors,
  minY: providedMinY,
  maxY: providedMaxY,
  formatXLabel,
  formatYLabel,
  formatTooltip,
  barPadding = 0.2,
  barBorderRadius = 4,
  onBarClick
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
    if (!svgRef.current || !containerRef.current || dimensions.width === 0 || !data.length) return;

    // Clear previous contents
    const svg = svgRef.current;
    while (svg.firstChild) {
      svg.removeChild(svg.firstChild);
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

    // Find min and max Y values
    let minY = 0; // Usually, bar charts start at 0
    let maxY = stacked 
      ? data.reduce((acc, d) => Math.max(acc, d.value), 0)
      : Math.max(...data.map(d => d.value));

    // Apply provided min/max if specified
    if (providedMinY !== undefined) minY = providedMinY;
    if (providedMaxY !== undefined) maxY = providedMaxY;

    // Add some padding to the Y domain
    const yPadding = (maxY - minY) * 0.1;
    maxY = Math.ceil(maxY + yPadding);

    // Create scales
    const xScale = (index: number) => {
      if (horizontal) {
        // For horizontal bars, x axis is the value
        return (index / data.length) * innerHeight;
      } else {
        // For vertical bars, x axis is the category
        const barWidth = innerWidth / data.length;
        return index * barWidth + barWidth / 2;
      }
    };

    const yScale = (value: number) => {
      if (horizontal) {
        // For horizontal bars, y axis is the value
        return (value - minY) / (maxY - minY) * innerWidth;
      } else {
        // For vertical bars, y axis is the value
        return innerHeight - ((value - minY) / (maxY - minY) * innerHeight);
      }
    };

    // Calculate bar width
    const effectiveBars = data.length;
    const barWidth = horizontal 
      ? (innerHeight / effectiveBars) * (1 - barPadding)
      : (innerWidth / effectiveBars) * (1 - barPadding);

    // Draw grid if enabled
    if (showGrid) {
      // Draw grid lines
      const yTickCount = 5;
      const yStep = (maxY - minY) / (yTickCount - 1);
      
      for (let i = 0; i < yTickCount; i++) {
        const yValue = minY + i * yStep;
        const y = horizontal ? 0 : yScale(yValue);
        const x = horizontal ? yScale(yValue) : 0;
        const lineLength = horizontal ? innerHeight : innerWidth;
        
        const gridLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        gridLine.setAttribute('x1', String(x));
        gridLine.setAttribute('y1', String(y));
        gridLine.setAttribute('x2', horizontal ? String(x) : String(innerWidth));
        gridLine.setAttribute('y2', horizontal ? String(innerHeight) : String(y));
        gridLine.setAttribute('class', 'chart-grid-line');
        g.appendChild(gridLine);
      }
    }

    // Draw X and Y axes
    // X axis
    const xAxis = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    xAxis.setAttribute('x1', '0');
    xAxis.setAttribute('y1', horizontal ? '0' : String(innerHeight));
    xAxis.setAttribute('x2', horizontal ? String(innerWidth) : String(innerWidth));
    xAxis.setAttribute('y2', horizontal ? String(innerHeight) : String(innerHeight));
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

    // Y axis
    const yAxis = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    yAxis.setAttribute('x1', '0');
    yAxis.setAttribute('y1', '0');
    yAxis.setAttribute('x2', horizontal ? '0' : '0');
    yAxis.setAttribute('y2', horizontal ? String(innerHeight) : String(innerHeight));
    yAxis.setAttribute('class', 'chart-axis');
    g.appendChild(yAxis);

    // Y axis title
    if (yAxisTitle) {
      const yTitle = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      
      if (horizontal) {
        yTitle.setAttribute('x', String(innerWidth / 2));
        yTitle.setAttribute('y', String(-margin.top / 2));
        yTitle.setAttribute('text-anchor', 'middle');
      } else {
        yTitle.setAttribute('transform', `rotate(-90) translate(${-innerHeight / 2}, ${-40})`);
        yTitle.setAttribute('text-anchor', 'middle');
      }
      
      yTitle.setAttribute('class', 'chart-axis-title');
      yTitle.textContent = yAxisTitle;
      g.appendChild(yTitle);
    }

    // X axis labels (categories)
    data.forEach((item, index) => {
      // Tick mark
      const tickX = horizontal ? 0 : xScale(index);
      const tickY = horizontal ? xScale(index) : innerHeight;
      
      // Label
      const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      
      if (horizontal) {
        label.setAttribute('x', '-10');
        label.setAttribute('y', String(tickY));
        label.setAttribute('text-anchor', 'end');
        label.setAttribute('dominant-baseline', 'middle');
      } else {
        label.setAttribute('x', String(tickX));
        label.setAttribute('y', String(tickY + 20));
        label.setAttribute('text-anchor', 'middle');
      }
      
      label.setAttribute('class', 'chart-tick-label');
      
      const formattedLabel = formatXLabel ? formatXLabel(item.label) : item.label;
      
      // Truncate long labels
      let displayedLabel = formattedLabel;
      if (displayedLabel.length > 10) {
        displayedLabel = displayedLabel.substring(0, 10) + '...';
      }
      
      label.textContent = displayedLabel;
      g.appendChild(label);
    });

    // Y axis ticks and labels
    const yTickCount = 5;
    const yStep = (maxY - minY) / (yTickCount - 1);

    for (let i = 0; i < yTickCount; i++) {
      const yValue = minY + i * yStep;
      const y = horizontal ? yScale(yValue) : yScale(yValue);
      
      // Tick mark
      const tick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      
      if (horizontal) {
        tick.setAttribute('x1', String(y));
        tick.setAttribute('y1', String(innerHeight));
        tick.setAttribute('x2', String(y));
        tick.setAttribute('y2', String(innerHeight + 6));
      } else {
        tick.setAttribute('x1', '-6');
        tick.setAttribute('y1', String(y));
        tick.setAttribute('x2', '0');
        tick.setAttribute('y2', String(y));
      }
      
      tick.setAttribute('class', 'chart-tick');
      g.appendChild(tick);

      // Label
      const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      
      if (horizontal) {
        label.setAttribute('x', String(y));
        label.setAttribute('y', String(innerHeight + 20));
        label.setAttribute('text-anchor', 'middle');
      } else {
        label.setAttribute('x', '-10');
        label.setAttribute('y', String(y));
        label.setAttribute('text-anchor', 'end');
        label.setAttribute('dominant-baseline', 'middle');
      }
      
      label.setAttribute('class', 'chart-tick-label');
      
      const formattedLabel = formatYLabel ? formatYLabel(yValue) : yValue.toLocaleString();
      label.textContent = formattedLabel;
      g.appendChild(label);
    }

    // Draw bars
    data.forEach((item, index) => {
      const color = item.color || colors[index % colors.length];
      const barRect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');

      if (horizontal) {
        const y = xScale(index) - barWidth / 2;
        const x = 0;
        const height = barWidth;
        const width = yScale(item.value);
        
        barRect.setAttribute('x', String(x));
        barRect.setAttribute('y', String(y));
        barRect.setAttribute('width', String(width));
        barRect.setAttribute('height', String(height));
        barRect.setAttribute('rx', String(barBorderRadius));
        barRect.setAttribute('ry', String(barBorderRadius));
      } else {
        const x = xScale(index) - barWidth / 2;
        const barHeight = innerHeight - yScale(item.value);
        const y = innerHeight - barHeight;
        
        barRect.setAttribute('x', String(x));
        barRect.setAttribute('y', String(y));
        barRect.setAttribute('width', String(barWidth));
        barRect.setAttribute('height', String(barHeight));
        barRect.setAttribute('rx', String(barBorderRadius));
        barRect.setAttribute('ry', String(barBorderRadius));
      }
      
      barRect.setAttribute('fill', color);
      barRect.setAttribute('class', 'chart-bar');
      
      // Animation
      if (animate) {
        if (horizontal) {
          barRect.style.transformOrigin = 'left center';
          barRect.style.animation = `chartBarAnimation 0.5s ease-out ${index * 0.05}s forwards`;
          barRect.style.transform = 'scaleX(0)';
        } else {
          barRect.style.transformOrigin = 'bottom center';
          barRect.style.animation = `chartBarAnimation 0.5s ease-out ${index * 0.05}s forwards`;
          barRect.style.transform = 'scaleY(0)';
        }
      }
      
      // Event handlers for tooltip and click
      if (showTooltip) {
        barRect.addEventListener('mouseenter', (e) => {
          if (!tooltipRef.current) return;
          
          const tooltipContent = formatTooltip
            ? formatTooltip(item.label, item.value)
            : `${item.label}: ${item.value.toLocaleString()}`;

          const rect = containerRef.current!.getBoundingClientRect();
          const offsetX = e.clientX - rect.left;
          const offsetY = e.clientY - rect.top;
          
          setTooltip({
            visible: true,
            x: offsetX,
            y: offsetY - 10,
            content: tooltipContent
          });
          
          // Highlight bar
          barRect.style.opacity = '0.8';
        });

        barRect.addEventListener('mouseleave', () => {
          setTooltip(prev => ({ ...prev, visible: false }));
          
          // Reset highlight
          barRect.style.opacity = '1';
        });
      }
      
      if (onBarClick) {
        barRect.style.cursor = 'pointer';
        barRect.addEventListener('click', () => {
          onBarClick(item);
        });
      }
      
      g.appendChild(barRect);
      
      // Add value label on top of the bar if there's enough space
      if (!horizontal && item.value > maxY * 0.05) {
        const barHeight = innerHeight - yScale(item.value);
        const valueLabel = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        valueLabel.setAttribute('x', String(xScale(index)));
        valueLabel.setAttribute('y', String(innerHeight - barHeight - 5));
        valueLabel.setAttribute('text-anchor', 'middle');
        valueLabel.setAttribute('class', 'chart-tick-label');
        valueLabel.textContent = formatYLabel ? formatYLabel(item.value) : item.value.toLocaleString();
        g.appendChild(valueLabel);
      }
    });

    // Create legend if enabled
    if (showLegend && data.length > 0) {
      const legendG = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      legendG.setAttribute('class', 'chart-legend');
      
      const legendItemHeight = 20;
      const legendItemWidth = 150;
      const legendPadding = 5;
      const legendItems = Math.min(data.length, 5); // Show max 5 items in one row
      
      data.forEach((item, i) => {
        const color = item.color || colors[i % colors.length];
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
        let itemLabel = item.label;
        if (itemLabel.length > 15) {
          itemLabel = itemLabel.substring(0, 15) + '...';
        }
        
        text.textContent = itemLabel;
        itemG.appendChild(text);
        
        legendG.appendChild(itemG);
      });
      
      // Calculate legend dimensions
      const legendRows = Math.ceil(data.length / legendItems);
      const legendHeight = legendRows * legendItemHeight + legendPadding * 2;
      const legendWidth = Math.min(data.length, legendItems) * legendItemWidth + legendPadding * 2;
      
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
  }, [
    dimensions, 
    data, 
    animate, 
    showGrid, 
    showLegend, 
    showTooltip, 
    horizontal, 
    xAxisTitle, 
    yAxisTitle, 
    formatXLabel, 
    formatYLabel, 
    formatTooltip,
    colors, 
    providedMinY, 
    providedMaxY,
    barPadding,
    barBorderRadius,
    onBarClick,
    stacked
  ]);

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

export default BarChart; 