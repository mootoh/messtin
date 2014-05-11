# auth_wrap.rb

require 'google/api_client'
require 'google/api_client/client_secrets'
require 'google/api_client/auth/file_storage'
require 'google/api_client/auth/installed_app'
require 'json'

class GDriveApp
  API_VERSION = 'v2'
  CACHED_API_FILE = "drive-#{API_VERSION}.cache"
  CREDENTIAL_STORE_FILE = "credential_store.json"

  attr_reader :client, :drive

  def initialize(name)
    @client = Google::APIClient.new(
      :application_name => name,
      :application_version => '0.0.1'
      )

    file_storage = Google::APIClient::FileStorage.new(CREDENTIAL_STORE_FILE)
    if file_storage.authorization.nil?
      client_secrets = Google::APIClient::ClientSecrets.load
      # The InstalledAppFlow is a helper class to handle the OAuth 2.0 installed
      # application flow, which ties in with FileStorage to store credentials
      # between runs.
      flow = Google::APIClient::InstalledAppFlow.new(
        :client_id => client_secrets.client_id,
        :client_secret => client_secrets.client_secret,
        :scope => ['https://www.googleapis.com/auth/drive']
        )
      @client.authorization = flow.authorize(file_storage)
    else
      @client.authorization = file_storage.authorization
    end

    @drive = nil
    # Load cached discovered API, if it exists. This prevents retrieving the
    # discovery document on every run, saving a round-trip to API servers.
    if File.exists? CACHED_API_FILE
      File.open(CACHED_API_FILE) do |file|
        @drive = Marshal.load(file)
      end
    else
      @drive = @client.discovered_api('drive', API_VERSION)
      File.open(CACHED_API_FILE, 'w') do |file|
        Marshal.dump(@drive, file)
      end
    end
  end

  def createFolder(name, description, into=nil)
    params = {
      'title' => name,
      'description' => description,
      'mimeType' => 'application/vnd.google-apps.folder',
    }
    params['parents'] = [{'id' => into}] if into

    folder = @drive.files.insert.request_schema.new(params)

    result = @client.execute(:api_method => drive.files.insert,
      :body_object => folder
    )

    result.data.id
  end

  def createFolderUnlessExists(name, description)
    id = fileExists(name)
    if id
      puts 'folder ' + name + ' already exists.'
      return id
    end

    createFolder(name, description)
  end

  def fileExists(name)
    result = client.execute(
      :api_method => @drive.files.list,
      :parameters => {
        'q' => 'title = "' + name + '"'
      }
      )

    return result.data.items.empty? ? false : result.data.items[0].id
  end

  def messtinFolder
    createFolderUnlessExists('messtin', 'Folder for Messtin app')
  end

  def uploadJpeg(filename, description, to)
    file = @drive.files.insert.request_schema.new({
      'title' => File.basename(filename),
      'description' => description,
      'mimeType' => 'image/jpeg',
      'parents' => [{'id' => to}]
      })

    media = Google::APIClient::UploadIO.new(filename, 'image/jpeg')

    result = @client.execute(
      :api_method => drive.files.insert,
      :body_object => file,
      :media => media,
      :parameters => {
        'uploadType' => 'multipart',
        'alt' => 'json'
        })

    return result.data.id
  end
end