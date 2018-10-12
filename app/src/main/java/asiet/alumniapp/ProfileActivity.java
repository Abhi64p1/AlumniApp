package asiet.alumniapp;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ProfileActivity extends AppCompatActivity
{

    private TextView mTextMessage;
    private ConstraintLayout LL;
    private int PhotoPickerRequestCode = 0;
    private BottomNavigationView navigation;
    private EntryAnimation EA;
    private Thread AnimationThread;
    private ImageView ProPicView;
    private String ProPicPath;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener()
    {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item)
        {
            switch (item.getItemId())
            {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    HomePressed();
                    return true;
                case R.id.navigation_account:
                    mTextMessage.setText(R.string.title_dashboard);
                    AccountPressed();
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    NotificationsPressed();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mTextMessage = (TextView) findViewById(R.id.message);
        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        LL = findViewById(R.id.ProfileLayout);
    }

    private void Logout()
    {
        SharedPreferences.Editor editor = getSharedPreferences(CommonData.SP,MODE_PRIVATE).edit();
        editor.putString("email","...");
        editor.putString("password","...");
        editor.putBoolean("LoggedIn",false);
        editor.apply();
        File Image = new File(ProPicPath);
        if(Image.exists())
            Image.delete();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("Status","Logout");
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    public void LogoutButtonPressed(View view)
    {
        Logout();
    }

    private void HomePressed()
    {
        LL.removeAllViewsInLayout();
        LL.addView(LayoutInflater.from(this).inflate(R.layout.profile_home,LL,false));
    }

    private void AccountPressed()
    {
        LL.removeAllViewsInLayout();
        LL.addView(LayoutInflater.from(this).inflate(R.layout.profile_account,LL,false));
        EA = findViewById(R.id.ProPicUpdateAnim);
        ProPicView = findViewById(R.id.ProPicView);
        ProPicPath = getExternalCacheDir().getAbsolutePath() + "/ProPic.webp";
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            LoadImageToView();
        else
        {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("App needs storage permission for showing profile picture from phone's storage.")
                    .setCancelable(false)
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
                        }
                    })
                    .create().show();
        }
    }

    private void NotificationsPressed()
    {
        LL.removeAllViewsInLayout();
        LL.addView(LayoutInflater.from(this).inflate(R.layout.profile_notifications,LL,false));
    }

    public void ProfilePhotoClicked(View view)
    {
        startActivityForResult(new Intent(this,ProfilePhotoPicker.class),PhotoPickerRequestCode);
    }

    private void UploadFile(final String Path)
    {
        try
        {
            String email = getSharedPreferences(CommonData.SP,MODE_PRIVATE).getString("email",null);
            String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            File Image = new File(Path);
            FileInputStream fileInputStream = new FileInputStream(Image);
            HttpsURLConnection connection = (HttpsURLConnection) new URL(CommonData.UploadImageAddress).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"img_upload\"; filename=\"" + email + ".webp" + "\"" + "\r\n");
            outputStream.writeBytes("Content-Type: image/jpeg" + "\r\n");
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + "\r\n");
            outputStream.writeBytes("\r\n");

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, 1048576);
            buffer = new byte[bufferSize];
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0)
            {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, 1048576);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            outputStream.writeBytes("\r\n");
            outputStream.writeBytes("--" + boundary + "--" + "\r\n");

            int status = connection.getResponseCode();
            if (status == HttpsURLConnection.HTTP_OK)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine);
                }
                connection.disconnect();
                fileInputStream.close();
                outputStream.flush();
                outputStream.close();

                final String Result = response.toString();
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (EA.isRunning)
                            StopAnimation();
                        if (Result.equals("Success"))
                            ProPicView.setImageBitmap(BitmapFactory.decodeFile(ProPicPath));
                        else
                            Toast.makeText(ProfileActivity.this, "Bad Internet Connection!", Toast.LENGTH_LONG).show();
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (EA.isRunning)
                            StopAnimation();
                        Toast.makeText(ProfileActivity.this, "Bad Internet Connection!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (final Exception ex)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (EA.isRunning)
                        StopAnimation();
                    Toast.makeText(ProfileActivity.this, "Bad Internet Connection!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void DownloadImage()
    {
        try
        {
            byte[] buffer = new byte[4096];
            int BytesRead;
            String email = getSharedPreferences(CommonData.SP,MODE_PRIVATE).getString("email",null);
            HttpsURLConnection connection = (HttpsURLConnection) new URL(CommonData.DownloadImageAddress).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
            writer.write("email=" + email);
            writer.flush();
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK)
            {
                File Image = new File(ProPicPath);
                Image.createNewFile();
                FileOutputStream ImageOutput = new FileOutputStream(Image);
                InputStream Input = connection.getInputStream();
                while((BytesRead = Input.read(buffer,0,4096))>0)
                    ImageOutput.write(buffer,0,BytesRead);
                ImageOutput.flush();
                ImageOutput.close();
                Input.close();
                connection.disconnect();
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(EA.isRunning)
                            StopAnimation();
                        ProPicView.setImageBitmap(BitmapFactory.decodeFile(ProPicPath));
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (EA.isRunning)
                            StopAnimation();
                        Toast.makeText(ProfileActivity.this, "Download Error.Bad Internet Connection!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        catch(final Exception ex)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (EA.isRunning)
                        StopAnimation();
                    Toast.makeText(ProfileActivity.this, "Download Error. Bad Internet Connection!\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void StoreImage(String Path)
    {
        try
        {
            Bitmap bitmap = BitmapFactory.decodeFile(Path);
            int Width = bitmap.getWidth();
            int Height = bitmap.getHeight();
            int CenterX = Width/2;
            int CenterY = Height/2;
            if(Width > Height)
                Width = Height;
            else
                Height = Width;
            bitmap = Bitmap.createBitmap(bitmap,CenterX-Width/2,CenterY-Height/2,Width,Height);
            bitmap = Bitmap.createScaledBitmap(bitmap,800,800,true);
            File Image = new File(ProPicPath);
            if (Image.exists())
                Image.delete();
            Image.createNewFile();
            OutputStream ImageOut = new FileOutputStream(Image);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 30,ImageOut);
            ImageOut.flush();
            ImageOut.close();
        }
        catch(final Exception ex)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(ProfileActivity.this, "Can't load image!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void LoadImageToView()
    {
        File Image = new File(ProPicPath);
        if (Image.exists())
            ProPicView.setImageBitmap(BitmapFactory.decodeFile(ProPicPath));
        else
        {
            Thread DownloadImageThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    DownloadImage();
                }
            });
            if (!EA.isRunning)
                StartAnimation();
            DownloadImageThread.start();
        }
    }

    private void StartAnimation()
    {
        EA.isRunning = true;
        EA.setVisibility(View.VISIBLE);
        AnimationThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    try
                    {
                        Thread.sleep(150);
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                EA.Change();
                            }
                        });
                    }
                    catch(Exception ex){ break; }
                }
            }
        });
        AnimationThread.start();
    }

    private void StopAnimation()
    {
        EA.isRunning = false;
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                EA.setVisibility(View.INVISIBLE);
            }
        });
        AnimationThread.interrupt();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data)
    {
        if (requestCode == PhotoPickerRequestCode)
        {
            if (resultCode == RESULT_OK)
            {
                if(!EA.isRunning)
                    StartAnimation();
                Thread BGThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        StoreImage(data.getStringExtra("FilePath"));
                        UploadFile(ProPicPath);
                    }
                });
                BGThread.start();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0)
        {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Denied")
                        .setMessage("Can't load profile photo without storage permission!")
                        .create().show();
            }
            else
                LoadImageToView();
        }
    }

}
