import { useEffect, useRef } from 'react';
import Editor, { type OnMount } from '@monaco-editor/react';
import { Box } from '@mui/material';
import { useColorMode } from '../theme/ColorModeProvider';

interface Props {
  content: string;
  language?: string | null;
  /** 1-based line to reveal + highlight when the editor mounts. */
  highlightStart?: number;
  highlightEnd?: number;
  height?: number | string;
}

/**
 * Read-only Monaco editor used to display code chunks and cited spans.
 * A highlight range is revealed and decorated when provided.
 */
export function CodeViewer({ content, language, highlightStart, highlightEnd, height = 360 }: Props) {
  const { mode } = useColorMode();
  const editorRef = useRef<Parameters<OnMount>[0] | null>(null);
  const monacoRef = useRef<Parameters<OnMount>[1] | null>(null);
  const decorationsRef = useRef<string[]>([]);

  const applyHighlight = () => {
    const editor = editorRef.current;
    const monaco = monacoRef.current;
    if (!editor || !monaco || !highlightStart) return;

    const end = highlightEnd ?? highlightStart;
    decorationsRef.current = editor.deltaDecorations(decorationsRef.current, [
      {
        range: new monaco.Range(highlightStart, 1, end, 1),
        options: {
          isWholeLine: true,
          className: 'cited-line-highlight',
          linesDecorationsClassName: 'cited-line-margin',
        },
      },
    ]);
    editor.revealLineInCenter(highlightStart);
  };

  const handleMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
    applyHighlight();
  };

  // Re-apply when the highlight target changes for an already-mounted editor.
  useEffect(() => {
    applyHighlight();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [highlightStart, highlightEnd, content]);

  return (
    <Box
      sx={{
        border: 1,
        borderColor: 'divider',
        borderRadius: 1,
        overflow: 'hidden',
        '& .cited-line-highlight': {
          backgroundColor: (t) =>
            t.palette.mode === 'dark' ? 'rgba(124,77,255,0.22)' : 'rgba(25,118,210,0.14)',
        },
        '& .cited-line-margin': {
          backgroundColor: 'primary.main',
          width: '4px !important',
          marginLeft: '3px',
        },
      }}
    >
      <Editor
        height={height}
        language={language ?? 'plaintext'}
        value={content}
        theme={mode === 'dark' ? 'vs-dark' : 'light'}
        onMount={handleMount}
        options={{
          readOnly: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          fontSize: 13,
          lineNumbers: 'on',
          renderLineHighlight: 'none',
          automaticLayout: true,
        }}
      />
    </Box>
  );
}
