/**
 * React Native Audio Player Component
 *
 * This example demonstrates smooth audio streaming from your Spring Boot backend
 * with support for seeking, playback controls, and error handling.
 *
 * Installation:
 * npm install react-native-track-player
 *
 * iOS Setup:
 * cd ios && pod install
 */

import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Alert,
} from 'react-native';
import TrackPlayer, {
  Capability,
  State,
  usePlaybackState,
  useProgress,
  Event,
  Track,
} from 'react-native-track-player';

// Configuration
const API_BASE_URL = 'https://your-api.com/api/v1';

interface Lecture {
  id: number;
  title: string;
  speakerName: string;
  duration: number; // in seconds
  filePath: string;
}

interface AudioPlayerProps {
  lecture: Lecture;
  authToken: string;
}

/**
 * Audio Player Component
 *
 * Features:
 * - Smooth streaming from R2 storage
 * - Seek support (range requests)
 * - Background playback
 * - Lock screen controls
 * - Error handling
 */
export const AudioPlayer: React.FC<AudioPlayerProps> = ({ lecture, authToken }) => {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const playbackState = usePlaybackState();
  const progress = useProgress();

  // Initialize player
  useEffect(() => {
    setupPlayer();
    return () => {
      TrackPlayer.reset();
    };
  }, []);

  // Load track when lecture changes
  useEffect(() => {
    if (lecture) {
      loadTrack();
    }
  }, [lecture.id]);

  /**
   * Setup the audio player with capabilities
   */
  const setupPlayer = async () => {
    try {
      await TrackPlayer.setupPlayer({
        waitForBuffer: true, // Wait for buffer before playing
        autoUpdateMetadata: true,
      });

      await TrackPlayer.updateOptions({
        capabilities: [
          Capability.Play,
          Capability.Pause,
          Capability.SeekTo,
          Capability.SkipToNext,
          Capability.SkipToPrevious,
        ],
        compactCapabilities: [Capability.Play, Capability.Pause, Capability.SeekTo],
        notificationCapabilities: [
          Capability.Play,
          Capability.Pause,
          Capability.SeekTo,
        ],
      });

      setIsLoading(false);
    } catch (err) {
      console.error('Failed to setup player:', err);
      setError('Failed to initialize audio player');
      setIsLoading(false);
    }
  };

  /**
   * Load audio track from backend
   *
   * The backend will stream the audio with range request support,
   * allowing smooth seeking and chunked transfer.
   */
  const loadTrack = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Get stream URL from backend
      const response = await fetch(
        `${API_BASE_URL}/lectures/${lecture.id}/stream-url`,
        {
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error('Failed to get stream URL');
      }

      const data = await response.json();

      // Construct streaming URL with token parameter
      // Token param is used because react-native-track-player can't set custom headers
      const streamUrl = `${API_BASE_URL}${data.url}?token=${authToken}`;

      // Create track object
      const track: Track = {
        id: lecture.id.toString(),
        url: streamUrl,
        title: lecture.title,
        artist: lecture.speakerName,
        duration: lecture.duration,
        // Optional: Add artwork
        // artwork: 'https://your-cdn.com/artwork.jpg',
      };

      // Reset player and add track
      await TrackPlayer.reset();
      await TrackPlayer.add(track);

      setIsLoading(false);
    } catch (err) {
      console.error('Failed to load track:', err);
      setError('Failed to load audio. Please try again.');
      setIsLoading(false);
      Alert.alert('Error', 'Failed to load audio. Please check your connection.');
    }
  };

  /**
   * Toggle play/pause
   */
  const togglePlayback = async () => {
    try {
      const state = await TrackPlayer.getState();

      if (state === State.Playing) {
        await TrackPlayer.pause();
      } else {
        await TrackPlayer.play();
      }
    } catch (err) {
      console.error('Playback error:', err);
      Alert.alert('Error', 'Playback control failed');
    }
  };

  /**
   * Seek to position (in seconds)
   */
  const seekTo = async (seconds: number) => {
    try {
      await TrackPlayer.seekTo(seconds);
    } catch (err) {
      console.error('Seek error:', err);
    }
  };

  /**
   * Skip forward 15 seconds
   */
  const skipForward = async () => {
    const newPosition = Math.min(progress.position + 15, progress.duration);
    await seekTo(newPosition);
  };

  /**
   * Skip backward 15 seconds
   */
  const skipBackward = async () => {
    const newPosition = Math.max(progress.position - 15, 0);
    await seekTo(newPosition);
  };

  /**
   * Format seconds to MM:SS
   */
  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  // Loading state
  if (isLoading) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>Loading audio...</Text>
      </View>
    );
  }

  // Error state
  if (error) {
    return (
      <View style={styles.container}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={loadTrack}>
          <Text style={styles.retryButtonText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const isPlaying = playbackState === State.Playing;
  const isBuffering = playbackState === State.Buffering;

  return (
    <View style={styles.container}>
      {/* Lecture Info */}
      <View style={styles.infoContainer}>
        <Text style={styles.title}>{lecture.title}</Text>
        <Text style={styles.artist}>{lecture.speakerName}</Text>
      </View>

      {/* Progress Bar */}
      <View style={styles.progressContainer}>
        <View style={styles.progressBar}>
          <View
            style={[
              styles.progressFill,
              {
                width: `${(progress.position / progress.duration) * 100}%`,
              },
            ]}
          />
        </View>
        <View style={styles.timeContainer}>
          <Text style={styles.timeText}>{formatTime(progress.position)}</Text>
          <Text style={styles.timeText}>{formatTime(progress.duration)}</Text>
        </View>
      </View>

      {/* Controls */}
      <View style={styles.controlsContainer}>
        {/* Skip Backward */}
        <TouchableOpacity
          style={styles.controlButton}
          onPress={skipBackward}
          disabled={isBuffering}
        >
          <Text style={styles.controlButtonText}>-15s</Text>
        </TouchableOpacity>

        {/* Play/Pause */}
        <TouchableOpacity
          style={[styles.controlButton, styles.playButton]}
          onPress={togglePlayback}
          disabled={isBuffering}
        >
          {isBuffering ? (
            <ActivityIndicator color="#FFF" />
          ) : (
            <Text style={styles.playButtonText}>
              {isPlaying ? '⏸' : '▶️'}
            </Text>
          )}
        </TouchableOpacity>

        {/* Skip Forward */}
        <TouchableOpacity
          style={styles.controlButton}
          onPress={skipForward}
          disabled={isBuffering}
        >
          <Text style={styles.controlButtonText}>+15s</Text>
        </TouchableOpacity>
      </View>

      {/* Buffering Indicator */}
      {isBuffering && (
        <View style={styles.bufferingContainer}>
          <ActivityIndicator size="small" color="#007AFF" />
          <Text style={styles.bufferingText}>Buffering...</Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#F5F5F5',
  },
  infoContainer: {
    alignItems: 'center',
    marginBottom: 30,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    textAlign: 'center',
    marginBottom: 5,
  },
  artist: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  progressContainer: {
    width: '100%',
    marginBottom: 30,
  },
  progressBar: {
    width: '100%',
    height: 4,
    backgroundColor: '#DDD',
    borderRadius: 2,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#007AFF',
  },
  timeContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 8,
  },
  timeText: {
    fontSize: 12,
    color: '#666',
  },
  controlsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 20,
  },
  controlButton: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#FFF',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  playButton: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#007AFF',
  },
  controlButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#007AFF',
  },
  playButtonText: {
    fontSize: 32,
    color: '#FFF',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  errorText: {
    fontSize: 16,
    color: '#FF3B30',
    textAlign: 'center',
    marginBottom: 20,
  },
  retryButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 30,
    paddingVertical: 12,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '600',
  },
  bufferingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 20,
    gap: 8,
  },
  bufferingText: {
    fontSize: 14,
    color: '#666',
  },
});

/**
 * Example Usage in Parent Component
 */
export const LectureScreen = () => {
  const [lecture, setLecture] = useState<Lecture | null>(null);
  const [authToken, setAuthToken] = useState<string>('');

  useEffect(() => {
    // Fetch lecture details and auth token
    fetchLectureDetails();
  }, []);

  const fetchLectureDetails = async () => {
    try {
      // Get auth token (e.g., from Clerk)
      const token = await getAuthToken();
      setAuthToken(token);

      // Fetch lecture
      const response = await fetch(`${API_BASE_URL}/lectures/123`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();
      setLecture(data);
    } catch (err) {
      console.error('Failed to fetch lecture:', err);
    }
  };

  const getAuthToken = async (): Promise<string> => {
    // Implement your auth token retrieval
    // For Clerk: await clerk.session.getToken()
    return 'your-jwt-token';
  };

  if (!lecture || !authToken) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" color="#007AFF" />
      </View>
    );
  }

  return (
    <View style={{ flex: 1 }}>
      <AudioPlayer lecture={lecture} authToken={authToken} />
    </View>
  );
};

/**
 * Background Playback Service (services/PlaybackService.ts)
 *
 * Add this to enable background playback:
 */
export const PlaybackService = async () => {
  TrackPlayer.addEventListener(Event.RemotePlay, () => TrackPlayer.play());
  TrackPlayer.addEventListener(Event.RemotePause, () => TrackPlayer.pause());
  TrackPlayer.addEventListener(Event.RemoteSeek, (event) => TrackPlayer.seekTo(event.position));
  TrackPlayer.addEventListener(Event.RemoteNext, () => {
    // Handle skip to next track
  });
  TrackPlayer.addEventListener(Event.RemotePrevious, () => {
    // Handle skip to previous track
  });
};

/**
 * Register the playback service in index.js:
 *
 * import TrackPlayer from 'react-native-track-player';
 * import { PlaybackService } from './services/PlaybackService';
 *
 * TrackPlayer.registerPlaybackService(() => PlaybackService);
 */
